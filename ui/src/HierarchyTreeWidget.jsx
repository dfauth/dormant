const { useEffect, useState } = React;

const isLeaf = (value) => value === null || typeof value !== "object";

const valueToText = (value) => {
  if (value === null) return "null";
  if (typeof value === "string") return value;
  return String(value);
};

const coerceValue = (input, previousValue) => {
  if (input === "null") return null;
  if (typeof previousValue === "number") {
    const parsed = Number(input);
    return Number.isNaN(parsed) ? previousValue : parsed;
  }
  if (typeof previousValue === "boolean") {
    if (input.toLowerCase() === "true") return true;
    if (input.toLowerCase() === "false") return false;
    return previousValue;
  }
  return input;
};

const updateAtPath = (current, path, nextValue) => {
  if (path.length === 0) return nextValue;
  const [head, ...rest] = path;
  if (Array.isArray(current)) {
    const copy = [...current];
    copy[head] = updateAtPath(copy[head], rest, nextValue);
    return copy;
  }
  return {
    ...current,
    [head]: updateAtPath(current?.[head], rest, nextValue)
  };
};

const sortByName = (left, right) =>
  String(left).localeCompare(String(right), undefined, { sensitivity: "base" });

const TreeNode = ({ name, value, path, onLeafCommit, expandAllState }) => {
  const childEntries = Array.isArray(value)
    ? value.map((item, index) => [index, [${index}], item])
    : (value && typeof value === "object")
    ? Object.entries(value)
        .sort(([leftKey], [rightKey]) => sortByName(leftKey, rightKey))
        .map(([key, item]) => [key, key, item])
    : [];

  const hasChildren = childEntries.length > 0;
  const [expanded, setExpanded] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [draftValue, setDraftValue] = useState(valueToText(value));

  useEffect(() => {
    setDraftValue(valueToText(value));
  }, [value]);

  useEffect(() => {
    if (expandAllState) {
      setExpanded(expandAllState.expanded);
    }
  }, [expandAllState]);

  const commitEdit = () => {
    if (draftValue !== valueToText(value)) {
      onLeafCommit(path, draftValue, value);
    }
    setIsEditing(false);
  };

  return (
    <li>
      <div className="tree-node-row">
        {hasChildren ? (
          <button className="toggle" onClick={() => setExpanded(!expanded)} aria-label="toggle">
            {expanded ? "-" : "+"}
          </button>
        ) : (
          <span className="leaf-spacer"></span>
        )}
        <span className="node-icon" aria-hidden>{hasChildren ? (expanded ? "📂" : "📁") : "📄"}</span>
        <span className="node-name">{name}</span>
        {isLeaf(value) && (
          isEditing ? (
            <input
              className="label-input"
              autoFocus
              value={draftValue}
              onChange={(event) => setDraftValue(event.target.value)}
              onBlur={commitEdit}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  commitEdit();
                } else if (event.key === "Escape") {
                  setDraftValue(valueToText(value));
                  setIsEditing(false);
                }
              }}
            />
          ) : (
            <span className="leaf-value" onDoubleClick={() => setIsEditing(true)}>{valueToText(value)}</span>
          )
        )}
      </div>
      {hasChildren && expanded && (
        <ul className="tree">
          {childEntries.map(([segment, childName, childValue]) => (
            <TreeNode
              key={${path.join(".")}-${String(segment)}-${childName}}
              name={childName}
              value={childValue}
              path={[...path, segment]}
              onLeafCommit={onLeafCommit}
              expandAllState={expandAllState}
            />
          ))}
        </ul>
      )}
    </li>
  );
};

const HierarchyTreeWidget = ({ hierarchyEndpoint, saveKeyParam = "key" }) => {
  const [data, setData] = useState({});
  const [error, setError] = useState("");
  const [status, setStatus] = useState("");
  const [expandAllState, setExpandAllState] = useState({ expanded: false, seq: 0 });

  useEffect(() => {
    fetch(hierarchyEndpoint)
      .then((response) => {
        if (!response.ok) {
          throw new Error("Failed to load hierarchy");
        }
        return response.json();
      })
      .then((json) => setData(json || {}))
      .catch((err) => setError(err.message));
  }, [hierarchyEndpoint]);

  const saveTrade = (cacheKey, trade) => {
    setStatus("Saving...");
    setError("");
    const saveUrl = ${hierarchyEndpoint}?${encodeURIComponent(saveKeyParam)}=${encodeURIComponent(cacheKey)};
    return fetch(saveUrl, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(trade)
    }).then((response) => {
      if (!response.ok) {
        throw new Error("Failed to save hierarchy");
      }
      return response.json();
    }).then((saved) => {
      setData(saved || {});
      setStatus("Saved");
    }).catch((err) => setError(err.message));
  };

  const handleLeafCommit = (cacheKey, path, inputValue, previousValue) => {
    const updatedTrade = updateAtPath(data[cacheKey], path, coerceValue(inputValue, previousValue));
    setData((current) => ({
      ...current,
      [cacheKey]: updatedTrade
    }));
    saveTrade(cacheKey, updatedTrade);
  };

  const handleExpandAll = (expanded) => {
    setExpandAllState((current) => ({ expanded, seq: current.seq + 1 }));
  };

  if (error) {
    return (
      <div>
        <div className="toolbar">
          <button onClick={() => handleExpandAll(true)}>Open All</button>
          <button onClick={() => handleExpandAll(false)}>Close All</button>
          {status && <span className="status">{status}</span>}
        </div>
        <div className="error">{error}</div>
      </div>
    );
  }

  return (
    <div>
      <div className="toolbar">
        <button onClick={() => handleExpandAll(true)}>Open All</button>
        <button onClick={() => handleExpandAll(false)}>Close All</button>
        {status && <span className="status">{status}</span>}
      </div>
      <ul className="tree">
        {Object.entries(data)
          .sort(([leftKey], [rightKey]) => sortByName(leftKey, rightKey))
          .map(([cacheKey, node]) => (
            <TreeNode
              key={cacheKey}
              name={cacheKey}
              value={node}
              path={[]}
              onLeafCommit={(path, inputValue, previousValue) => handleLeafCommit(cacheKey, path, inputValue, previousValue)}
              expandAllState={expandAllState}
            />
          ))}
      </ul>
    </div>
  );
};

window.HierarchyTreeWidget = HierarchyTreeWidget;