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

const TreeNode = ({ name, value, path, onLeafCommit, expandAllState, mode, nodeCacheKey }) => {
  const isReadOnly = mode === "read-only";
  const childEntries = Array.isArray(value)
    ? value.map((item, index) => [index, [index], item])
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
      onLeafCommit(path, draftValue, value, nodeCacheKey);
    }
    setIsEditing(false);
  };

  const startEditing = () => {
    if (!isReadOnly) {
      setIsEditing(true);
    }
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
        <span className="name-value-separator">:</span>
        {isLeaf(value) ? (
          isEditing ? (
            typeof value === "boolean" || value === null ? (
              <select
                className="label-input boolean-select"
                autoFocus
                value={draftValue}
                onChange={(event) => setDraftValue(event.target.value)}
                onBlur={commitEdit}
                onKeyDown={(event) => {
                  if (event.key === "Escape") {
                    setDraftValue(valueToText(value));
                    setIsEditing(false);
                  }
                }}
                disabled={isReadOnly}
              >
                <option value="true">true</option>
                <option value="false">false</option>
                <option value="null">null</option>
              </select>
            ) : (
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
                disabled={isReadOnly}
              />
            )
          ) : (
            <span 
              className={`node-value ${!isReadOnly ? "editable-value" : ""}`}
              onClick={startEditing}
            >
              {valueToText(value)}
            </span>
          )
        ) : !hasChildren ? (
          <span className="node-value">{valueToText(value)}</span>
        ) : null}
      </div>
      {hasChildren && expanded && (
        <ul className="tree">
          {childEntries.map(([segment, childName, childValue]) => (
            <TreeNode
              key={`${path.join(".")}-${String(segment)}-${childName}`}
              name={childName}
              value={childValue}
              path={[...path, segment]}
              onLeafCommit={onLeafCommit}
              expandAllState={expandAllState}
              mode={mode}
              nodeCacheKey={nodeCacheKey}
            />
          ))}
        </ul>
      )}
    </li>
  );
};

const HierarchyTreeWidget = ({ 
  hierarchyEndpoint, 
  saveKeyParam = "key", 
  authToken,
  mode = "read-only",
  saveEndpoint 
}) => {
  const [data, setData] = useState({});
  const [error, setError] = useState("");
  const [status, setStatus] = useState("");
  const [expandAllState, setExpandAllState] = useState({ expanded: false, seq: 0 });

  const fetchWithAuth = (url, options = {}) => {
    const headers = new Headers(options.headers || {});
    if (authToken) {
      headers.append("Authorization", `Bearer ${authToken}`);
    }
    return fetch(url, { ...options, headers, credentials: "include" });
  };

  useEffect(() => {
    fetchWithAuth(hierarchyEndpoint)
      .then((response) => {
        if (!response.ok) {
          throw new Error("Failed to load hierarchy");
        }
        return response.json();
      })
      .then((json) => setData(json || {}))
      .catch((err) => setError(err.message));
  }, [hierarchyEndpoint, authToken]);

  const saveTrade = (cacheKey, trade, mode, path = [], inputValue, previousValue) => {
    setStatus("Saving...");
    setError("");
    
    if (mode === "writable-all") {
      // Save entire hierarchy
      const saveUrl = saveEndpoint || hierarchyEndpoint;
      return fetchWithAuth(saveUrl, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(trade)
      })
      .then(checkResponse)
      .then((saved) => {
        setData(saved || {});
        setStatus("Saved");
      })
      .catch((err) => setError(err.message));
    } else if (mode === "writable-slice") {
      // Save node being edited plus all parent nodes
      // For now, send the full hierarchy (slice implementation would need backend support)
      const saveUrl = saveEndpoint || hierarchyEndpoint;
      return fetchWithAuth(saveUrl, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(trade)
      })
      .then(checkResponse)
      .then((saved) => {
        setData(saved || {});
        setStatus("Saved");
      })
      .catch((err) => setError(err.message));
    } else if (mode === "writable-node") {
      // Save only the node being changed with path info
      const nodeValue = coerceValue(inputValue, previousValue);
      const payload = {
        path: path,
        value: nodeValue,
        cacheKey: cacheKey
      };
      const saveUrl = saveEndpoint || hierarchyEndpoint;
      return fetchWithAuth(saveUrl, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      })
      .then(checkResponse)
      .then((saved) => {
        setData(saved || {});
        setStatus("Saved");
      })
      .catch((err) => setError(err.message));
    }
    return Promise.resolve();
  };

  const checkResponse = (response) => {
    if (!response.ok) {
      throw new Error(`Failed to save: ${response.status}`);
    }
    return response.json();
  };

  const handleLeafCommit = (path, inputValue, previousValue, cacheKey) => {
    if (mode === "read-only") return;
    
    const updatedTrade = updateAtPath(data[cacheKey], path, coerceValue(inputValue, previousValue));
    setData((current) => ({
      ...current,
      [cacheKey]: updatedTrade
    }));
    saveTrade(cacheKey, updatedTrade, mode, path, inputValue, previousValue);
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
              onLeafCommit={(path, inputValue, previousValue) => handleLeafCommit(path, inputValue, previousValue, cacheKey)}
              expandAllState={expandAllState}
              mode={mode}
              nodeCacheKey={cacheKey}
            />
          ))}
      </ul>
    </div>
  );
};

window.HierarchyTreeWidget = HierarchyTreeWidget;