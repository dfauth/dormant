const { useEffect, useState, useCallback, useRef } = React;

// Parse formula in the format: =FETCH("url").field or =FETCH("url").map(...)
const parseFormula = (formula) => {
  if (!formula || !formula.startsWith("=")) {
    return null;
  }
  
  const text = formula.substring(1); // Remove '='
  
  // Match simple FETCH pattern: FETCH("url") or FETCH("url").field
  const simpleFetchMatch = text.match(/^FETCH\("([^"]+)"\)(\.(\w+))?$/);
  if (simpleFetchMatch) {
    return {
      type: "FETCH",
      url: simpleFetchMatch[1],
      field: simpleFetchMatch[3] || null,
      transform: null
    };
  }
  
  // Match FETCH with JavaScript: FETCH("url").map(...) or FETCH("url").filter(...)
  const jsFetchMatch = text.match(/^FETCH\("([^"]+)"\)\.(.+)$/);
  if (jsFetchMatch) {
    return {
      type: "FETCH_JS",
      url: jsFetchMatch[1],
      jsExpression: jsFetchMatch[2]
    };
  }
  
  // Match field extraction: .field
  const fieldMatch = text.match(/^\.(\w+)$/);
  if (fieldMatch) {
    return {
      type: "FIELD",
      field: fieldMatch[1]
    };
  }
  
  return null;
};

// Client-side cache for FETCH results
const fetchCache = {};

// Fetch data from URL with caching
const fetchData = async (url, authToken) => {
  // Check cache first
  if (fetchCache[url]) {
    return fetchCache[url];
  }
  
  const headers = new Headers();
  if (authToken) {
    headers.append("Authorization", `Bearer ${authToken}`);
  }
  
  const response = await fetch(url, {
    headers,
    credentials: "include"
  });
  
  if (!response.ok) {
    throw new Error(`Failed to fetch: ${response.status}`);
  }
  
  const data = await response.json();
  
  // Store in cache
  fetchCache[url] = data;
  
  return data;
};

// Evaluate JavaScript expression on data
const evaluateJsExpression = (data, expression) => {
  if (!expression) {
    return data;
  }
  try {
    // Use Function constructor for safer evaluation
    // This prevents access to global scope
    // Prepend 'data.' to the expression to make it a method call
    const code = `return data.${expression}`;
    const func = new Function('data', code);
    return func(data);
  } catch (e) {
    console.error("Error evaluating expression:", expression, "Error:", e);
    return data; // Return original data on error
  }
};

// Evaluate a formula
const evaluateFormula = async (formula, dataContext, authToken) => {
  const parsed = parseFormula(formula);
  
  if (!parsed) {
    return formula; // Return as-is if not a valid formula
  }
  
  if (parsed.type === "FETCH" || parsed.type === "FETCH_JS") {
    let data = await fetchData(parsed.url, authToken);
    
    // Ensure data is an array for array operations
    if (data && !Array.isArray(data)) {
      data = [data];
    }
    
    if (parsed.type === "FETCH_JS") {
      // Execute JavaScript expression on the data
      try {
        return evaluateJsExpression(data, parsed.jsExpression);
      } catch (e) {
        return data;
      }
    }
    
    if (parsed.field) {
      // Extract specific field from each object
      if (Array.isArray(data)) {
        return data.map(item => item[parsed.field]).filter(x => x !== undefined);
      } else if (data && typeof data === "object") {
        // Handle map/dictionary response - extract field from each value
        const values = Object.values(data);
        if (values.length > 0 && values[0] && typeof values[0] === "object") {
          // Each value is an object, extract the field from each
          return values.map(item => item[parsed.field]).filter(x => x !== undefined);
        } else {
          // Single object, extract the field
          return [data[parsed.field]];
        }
      }
    } else {
      // No field specified - return all values if it's a map, or the array
      if (Array.isArray(data)) {
        return data;
      } else if (data && typeof data === "object") {
        // For a map, return the values as an array
        return Object.values(data);
      }
    }
    
    // Return all data if no field specified
    return data;
  }
  
  if (parsed.type === "FIELD") {
    // Extract field from data context
    if (dataContext && typeof dataContext === "object") {
      return dataContext[parsed.field];
    }
  }
  
  return formula;
};

// Extract values from data for display
const extractValues = (data) => {
  if (Array.isArray(data)) {
    return data;
  }
  if (data && typeof data === "object") {
    return Object.values(data);
  }
  return [data];
};

// Extract column variable references from a formula
// Returns array of { name, columnIndex } for variables that match header names
const extractColumnVariables = (formula, headerNames) => {
  if (!formula || !formula.startsWith("=") || !headerNames) {
    return [];
  }
  
  const variables = [];
  const text = formula.substring(1); // Remove '='
  
  // Match FETCH formulas to check URL part
  const fetchMatch = text.match(/^FETCH\(([^)]+)\)/);
  if (fetchMatch) {
    const urlPart = fetchMatch[1];
    
    // Split by + to find potential variable references
    const parts = urlPart.split('+');
    
    headerNames.forEach((name, idx) => {
      if (name && /^[a-zA-Z_][a-zA-Z0-9_]*$/.test(name)) {
        // Check if any part matches this variable name exactly
        if (parts.some(part => part.trim() === name)) {
          variables.push({ name, columnIndex: idx });
        }
      }
    });
  }
  
  return variables;
};

// Evaluate a formula with row context for column variable resolution
// Used for formulas like =FETCH("http://.../"+asxCode).size
const evaluateFormulaWithContext = async (formula, rowData, headerNames, authToken) => {
  if (!formula || !formula.startsWith("=")) {
    return formula;
  }
  
  const text = formula.substring(1); // Remove '='
  const variables = extractColumnVariables(formula, headerNames);
  
  // If no variables, use standard evaluation
  if (variables.length === 0) {
    return evaluateFormula(formula, null, authToken);
  }
  
  // Handle FETCH with variables
  const fetchMatch = text.match(/^FETCH\(([^)]+)\)(\.(.+))?$/);
  if (fetchMatch) {
    let urlTemplate = fetchMatch[1];
    const afterFetch = fetchMatch[3] || '';
    
    // Build the URL by properly handling string literals and variable concatenation
    // Split by +, then process each part separately
    const parts = urlTemplate.split('+');
    
    const urlParts = parts.map(part => {
      const trimmed = part.trim();
      
      // Check if this part is a variable reference
      const varRef = variables.find(v => v.name === trimmed);
      if (varRef) {
        const value = rowData[varRef.columnIndex];
        if (value !== null && value !== undefined) {
          return String(value);
        }
        return '';
      }
      
      // It's a string literal - unquote it
      if (trimmed.startsWith('"') && trimmed.endsWith('"')) {
        return trimmed.slice(1, -1);
      }
      if (trimmed.startsWith("'") && trimmed.endsWith("'")) {
        return trimmed.slice(1, -1);
      }
      
      // If it has quotes in the middle, keep it as is
      return trimmed;
    });
    
    // Join the parts - the + was the concatenation operator in the formula
    let url = urlParts.join('');
    
    // Fetch data from the constructed URL
    try {
      let data = await fetchData(url, authToken);
      
      // Ensure data is an array for array operations
      if (data && !Array.isArray(data)) {
        data = [data];
      }
      
      // If there's a field or JS expression after FETCH
      if (afterFetch) {
        if (afterFetch.includes('(') || afterFetch.includes('=>') || 
            (afterFetch.includes('.') && !/^\.\w+$/.test(afterFetch))) {
          // JS expression
          return evaluateJsExpression(data, afterFetch);
        } else if (afterFetch.startsWith('.')) {
          // Simple field extraction
          const field = afterFetch.substring(1);
          if (Array.isArray(data)) {
            return data.map(item => item[field]).filter(x => x !== undefined);
          } else if (data && typeof data === "object") {
            return data[field];
          }
        }
      }
      
      return data;
    } catch (e) {
      console.error("Error evaluating dynamic FETCH:", e);
      return null;
    }
  }
  
  // For non-FETCH formulas with variables, try to evaluate with context
  return formula;
};

// Helper function to sort data by a column
const getSortedData = (data, sortColumn, sortDirection) => {
  if (sortColumn === null || sortDirection === null || !data || data.length === 0) {
    return data;
  }
  
  return [...data].sort((a, b) => {
    const valA = a[sortColumn];
    const valB = b[sortColumn];
    
    // Handle null/undefined
    if (valA === null || valA === undefined) return sortDirection === 'asc' ? 1 : -1;
    if (valB === null || valB === undefined) return sortDirection === 'asc' ? -1 : 1;
    
    // Numeric comparison
    if (typeof valA === 'number' && typeof valB === 'number') {
      return sortDirection === 'asc' ? valA - valB : valB - valA;
    }
    
    // String comparison
    const strA = String(valA).toLowerCase();
    const strB = String(valB).toLowerCase();
    
    if (strA < strB) return sortDirection === 'asc' ? -1 : 1;
    if (strA > strB) return sortDirection === 'asc' ? 1 : -1;
    return 0;
  });
};

// Helper function to filter data based on column filters
const getFilteredData = (data, filters) => {
  if (!filters || Object.keys(filters).length === 0 || !data || data.length === 0) {
    return data;
  }
  
  return data.filter(row => {
    // Check all filters - AND logic
    return Object.entries(filters).every(([colIndex, filterText]) => {
      if (!filterText) return true; // Empty filter means no filtering
      
      const col = parseInt(colIndex, 10);
      const value = row[col];
      
      if (value === null || value === undefined) return false;
      
      const strValue = String(value).toLowerCase();
      const strFilter = filterText.toLowerCase();
      
      return strValue.includes(strFilter);
    });
  });
};

// Context menu component
const ContextMenu = ({ 
  position, 
  onClose, 
  onEditHeader, 
  onEditFormula,
  onFreezeColumn,
  isFrozen,
  onFilterChange,
  onFilterClear,
  filterValue,
  colIndex
}) => {
  if (!position) return null;
  
  const handleClick = (e) => {
    e.stopPropagation();
  };
  
  return (
    <div 
      className="context-menu-overlay"
      onClick={onClose}
      style={{
        position: "fixed",
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        zIndex: 1000
      }}
    >
      <div 
        className="context-menu"
        onClick={handleClick}
        style={{
          position: "absolute",
          top: `${position.y}px`,
          left: `${position.x}px`,
          backgroundColor: "white",
          border: "1px solid #ccc",
          borderRadius: "4px",
          boxShadow: "0 2px 10px rgba(0,0,0,0.2)",
          padding: "8px 0",
          minWidth: "200px",
          zIndex: 1001
        }}
      >
        <div 
          className="context-menu-item"
          onClick={() => {
            onEditHeader();
            onClose();
          }}
          style={{
            padding: "8px 16px",
            cursor: "pointer",
            fontSize: "12px",
            color: "#333",
            textAlign: "left"
          }}
          onMouseOver={(e) => e.target.style.backgroundColor = "#f0f4f8"}
          onMouseOut={(e) => e.target.style.backgroundColor = ""}
        >
          Edit column name
        </div>
        <div 
          className="context-menu-item"
          onClick={() => {
            onEditFormula();
            onClose();
          }}
          style={{
            padding: "8px 16px",
            cursor: "pointer",
            fontSize: "12px",
            color: "#333",
            textAlign: "left"
          }}
          onMouseOver={(e) => e.target.style.backgroundColor = "#f0f4f8"}
          onMouseOut={(e) => e.target.style.backgroundColor = ""}
        >
          Edit formula
        </div>
        <hr style={{ margin: "4px 0", border: "none", borderTop: "1px solid #ddd" }} />
        {onFilterChange && (
          <div style={{ padding: "4px 12px" }}>
            <input
              type="text"
              className="filter-input-in-menu"
              placeholder="Type to filter..."
              value={filterValue || ''}
              onChange={(e) => onFilterChange(colIndex, e.target.value)}
              onClick={(e) => e.stopPropagation()}
              onKeyDown={(e) => e.stopPropagation()}
              style={{
                width: "100%",
                padding: "4px 6px",
                border: "1px solid #ccc",
                borderRadius: "3px",
                fontSize: "12px",
                boxSizing: "border-box"
              }}
            />
            {filterValue && (
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  onFilterClear(colIndex);
                }}
                style={{
                  marginTop: "4px",
                  padding: "2px 6px",
                  fontSize: "11px",
                  background: "none",
                  border: "1px solid #ccc",
                  borderRadius: "3px",
                  cursor: "pointer"
                }}
              >
                Clear
              </button>
            )}
          </div>
        )}
        {onFreezeColumn && (
          <>
            <hr style={{ margin: "4px 0", border: "none", borderTop: "1px solid #ddd" }} />
            <div 
              className="context-menu-item"
              onClick={() => {
                onFreezeColumn();
                onClose();
              }}
              style={{
                padding: "8px 16px",
                cursor: "pointer",
                fontSize: "12px",
                color: "#333",
                textAlign: "left"
              }}
              onMouseOver={(e) => e.target.style.backgroundColor = "#f0f4f8"}
              onMouseOut={(e) => e.target.style.backgroundColor = ""}
            >
              {isFrozen ? "Unfreeze first column" : "Freeze first column"}
            </div>
          </>
        )}
      </div>
    </div>
  );
};

const HeaderCell = ({ 
  value, 
  headerName, 
  onHeaderNameChange, 
  onFormulaChange,
  isFirstRow,
  colIndex,
  onResize,
  width,
  onFreezeColumn,
  isFrozen,
  onSort,
  sortColumn,
  sortDirection,
  onDragStart,
  onDragOver,
  onDragLeave,
  onDrop,
  onDragEnd,
  draggedColumn,
  dragOverColumn,
  onFilterChange,
  onFilterClear,
  filters
}) => {
  const [isEditing, setIsEditing] = useState(false);
  const [draftValue, setDraftValue] = useState(headerName || "");
  const [isEditingFormula, setIsEditingFormula] = useState(false);
  const [draftFormula, setDraftFormula] = useState(value || "");
  const [contextMenuPos, setContextMenuPos] = useState(null);
  const [isResizing, setIsResizing] = useState(false);
  const cellRef = useRef(null);
  const startXRef = useRef(null);
  const startWidthRef = useRef(null);
  
  useEffect(() => {
    setDraftValue(headerName || "");
  }, [headerName]);
  
  useEffect(() => {
    setDraftFormula(value || "");
  }, [value]);
  

  
  const handleContextMenu = (e) => {
    e.preventDefault();
    if (cellRef.current) {
      const rect = cellRef.current.getBoundingClientRect();
      setContextMenuPos({ x: rect.left, y: rect.bottom });
    }
  };
  
  const handleCloseContextMenu = () => {
    setContextMenuPos(null);
  };
  
  const handleEditHeader = () => {
    setIsEditing(true);
    setIsEditingFormula(false);
    handleCloseContextMenu();
  };
  
  const handleEditFormula = () => {
    setIsEditing(false);
    setIsEditingFormula(true);
    handleCloseContextMenu();
  };
  
  const handleFreezeColumn = () => {
    // Toggle freeze for first column only
    onFreezeColumn();
    handleCloseContextMenu();
  };
  
  const displayText = headerName || value || "";
  
  // Column resizing handlers
  const startResize = (e) => {
    if (e.target.classList.contains('resize-handle') || 
        (e.target === cellRef.current && e.clientX > cellRef.current.offsetWidth - 8)) {
      e.preventDefault();
      e.stopPropagation();
      setIsResizing(true);
      startXRef.current = e.clientX;
      startWidthRef.current = cellRef.current.offsetWidth;
      document.addEventListener('mousemove', handleResize);
      document.addEventListener('mouseup', stopResize);
    }
  };
  
  const handleResize = (e) => {
    if (!isResizing || !cellRef.current) return;
    e.preventDefault();
    const newWidth = startWidthRef.current + (e.clientX - startXRef.current);
    if (newWidth > 30) {
      cellRef.current.style.width = `${newWidth}px`;
      // Apply to all cells in this column
      const cells = cellRef.current.parentElement.querySelectorAll(`td:nth-child(${colIndex + 1}), th:nth-child(${colIndex + 1})`);
      cells.forEach(cell => cell.style.width = `${newWidth}px`);
    }
  };
  
  const stopResize = (e) => {
    if (isResizing) {
      setIsResizing(false);
      document.removeEventListener('mousemove', handleResize);
      document.removeEventListener('mouseup', stopResize);
      if (onResize && cellRef.current) {
        onResize(colIndex, cellRef.current.offsetWidth);
      }
    }
  };
  
  useEffect(() => {
    return () => {
      document.removeEventListener('mousemove', handleResize);
      document.removeEventListener('mouseup', stopResize);
    };
  }, []);
  
  // Determine sort indicator
  const isSorted = sortColumn === colIndex;
  const sortIndicator = isSorted 
    ? (sortDirection === 'asc' ? ' ↑' : ' ↓')
    : '';
  
  const handleClick = () => {
    if (onSort) {
      onSort(colIndex);
    }
  };

  // Determine if this column is being dragged over
  const isDragOver = dragOverColumn === colIndex;
  const isBeingDragged = draggedColumn === colIndex;

  return (
    <th 
      ref={cellRef}
      className={`table-header-cell ${isFrozen ? 'frozen-column' : ''} ${isSorted ? 'sorted-column' : ''} ${isDragOver ? 'drag-over' : ''} ${isBeingDragged ? 'being-dragged' : ''}`}
      onContextMenu={handleContextMenu}
      onMouseDown={startResize}
      onClick={handleClick}
      draggable={true}
      onDragStart={() => onDragStart(colIndex)}
      onDragOver={(e) => onDragOver(e, colIndex)}
      onDragLeave={onDragLeave}
      onDrop={(e) => onDrop(e, colIndex)}
      onDragEnd={onDragEnd}
      style={{
        backgroundColor: isFirstRow ? "#4a6fa5" : "#e9ecef",
        color: isFirstRow ? "white" : "#212529",
        cursor: isResizing ? "col-resize" : isBeingDragged ? "grabbing" : "pointer",
        width: width ? `${width}px` : undefined,
        opacity: isBeingDragged ? 0.5 : 1
      }}
    >
      <div style={{ position: "relative" }}>
        {isEditing ? (
          <input
            className="label-input"
            autoFocus
            value={draftValue}
            onChange={(e) => setDraftValue(e.target.value)}
            onBlur={() => {
              onHeaderNameChange(draftValue);
              setIsEditing(false);
            }}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                onHeaderNameChange(draftValue);
                setIsEditing(false);
              } else if (e.key === "Escape") {
                setDraftValue(headerName || "");
                setIsEditing(false);
              }
            }}
          />
        ) : isEditingFormula ? (
          <input
            className="label-input"
            autoFocus
            value={draftFormula}
            onChange={(e) => setDraftFormula(e.target.value)}
            onBlur={() => {
              onFormulaChange(draftFormula);
              setIsEditingFormula(false);
            }}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                onFormulaChange(draftFormula);
                setIsEditingFormula(false);
              } else if (e.key === "Escape") {
                setDraftFormula(value || "");
                setIsEditingFormula(false);
              }
            }}
          />
        ) : (
          <span className="header-text">
            {displayText || "Column"}{sortIndicator}
          </span>
        )}
        <div 
          className="resize-handle"
          onMouseDown={startResize}
          style={{
            position: "absolute",
            top: 0,
            right: 0,
            width: "4px",
            height: "100%",
            cursor: "col-resize",
            zIndex: 10
          }}
        />
      </div>
      
      <ContextMenu
        position={contextMenuPos}
        onClose={handleCloseContextMenu}
        onEditHeader={handleEditHeader}
        onEditFormula={handleEditFormula}
        onFreezeColumn={handleFreezeColumn}
        isFrozen={isFrozen}
        onFilterChange={onFilterChange}
        onFilterClear={onFilterClear}
        filterValue={filters && filters[colIndex] ? filters[colIndex] : ''}
        colIndex={colIndex}
      />
    </th>
  );
};



const ProgrammableTable = ({ 
  initialColumns = 5, 
  initialRows = 10,
  authToken,
  tableId = "default",
  settingsEndpoint = "/api/settings"
}) => {
  // Initialize with first column having a sample FETCH formula
  const initialFormulas = Array(initialColumns).fill(null);
  initialFormulas[0] = '=FETCH("http://localhost:8081/api/positions").code';
  
  // Initialize header names
  const initialHeaderNames = Array(initialColumns).fill(null);
  initialHeaderNames[0] = "Code";
  
  const [columns, setColumns] = useState(initialColumns);
  const [rows, setRows] = useState(initialRows);
  const [formulas, setFormulas] = useState(initialFormulas);
  const [headerNames, setHeaderNames] = useState(initialHeaderNames);
  const [columnWidths, setColumnWidths] = useState({});
  const [data, setData] = useState(Array(initialRows).fill(null).map(() => Array(initialColumns).fill(null)));
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [savingState, setSavingState] = useState(false);
  const [stateError, setStateError] = useState(null);
  const [tableStateLoaded, setTableStateLoaded] = useState(false);
  const [isEvaluating, setIsEvaluating] = useState(false);
  const [freezeFirstColumn, setFreezeFirstColumn] = useState(true); // Freeze first column
  const [freezeHeader, setFreezeHeader] = useState(true); // Freeze header row
  
  // Sorting state
  const [sortColumn, setSortColumn] = useState(null);
  const [sortDirection, setSortDirection] = useState(null); // 'asc' or 'desc'
  
  // Filtering state - object mapping column index to filter text
  const [filters, setFilters] = useState({});
  
  // Drag and drop state for column reordering
  const [draggedColumn, setDraggedColumn] = useState(null);
  const [dragOverColumn, setDragOverColumn] = useState(null);
  
  // Generate setting key for this table
  const settingKey = `${tableId}_tableState`;
  
  // Save table state to settings endpoint
  const saveTableState = useCallback(async () => {
    if (!settingsEndpoint) return;
    
    setSavingState(true);
    setStateError(null);
    
    try {
      const state = {
        columns,
        rows,
        formulas,
        headerNames,
        columnWidths,
        freezeFirstColumn,
        freezeHeader,
        filters,
        sortColumn,
        sortDirection
      };
      
      const headers = new Headers();
      if (authToken) {
        headers.append("Authorization", `Bearer ${authToken}`);
      }
      
      // POST to /api/settings/{key} with body as just the value
      const url = `${settingsEndpoint}/${encodeURIComponent(settingKey)}`;
      const response = await fetch(url, {
        method: "POST",
        headers: {
          ...Object.fromEntries(headers),
          "Content-Type": "application/json"
        },
        credentials: "include",
        body: JSON.stringify(state)
      });
      
      if (!response.ok) {
        throw new Error(`Failed to save table state: ${response.status}`);
      }
    } catch (e) {
      setStateError(e.message);
    } finally {
      setSavingState(false);
    }
  }, [settingsEndpoint, authToken, settingKey, columns, rows, formulas, headerNames, columnWidths]);
  
  // Load table state from settings endpoint
  const loadTableState = useCallback(async () => {
    if (!settingsEndpoint) return;
    
    try {
      const headers = new Headers();
      if (authToken) {
        headers.append("Authorization", `Bearer ${authToken}`);
      }
      
      // GET from /api/settings/{key}
      const url = `${settingsEndpoint}/${encodeURIComponent(settingKey)}`;
      const response = await fetch(url, {
        headers,
        credentials: "include"
      });
      
      if (response.ok) {
        const state = await response.json();
        if (state) {
          setColumns(state.columns || initialColumns);
          setRows(state.rows || initialRows);
          setFormulas(state.formulas || initialFormulas);
          setHeaderNames(state.headerNames || initialHeaderNames);
          setColumnWidths(state.columnWidths || {});
          setFreezeFirstColumn(state.freezeFirstColumn !== false);
          setFreezeHeader(state.freezeHeader !== false);
          setFilters(state.filters || {});
          setSortColumn(state.sortColumn || null);
          setSortDirection(state.sortDirection || null);
          
          // Initialize data based on loaded state
          const newData = Array(state.rows || initialRows).fill(null).map(() => 
            Array(state.columns || initialColumns).fill(null)
          );
          setData(newData);
        }
      }
    } catch (e) {
      // Silently fail - use defaults
      console.warn("Failed to load table state:", e.message);
    } finally {
      setTableStateLoaded(true);
    }
  }, [settingsEndpoint, authToken, settingKey, initialColumns, initialRows, initialFormulas, initialHeaderNames]);
  
  // Evaluate all formulas and populate data
  const evaluateAllFormulas = useCallback(async () => {
    // Prevent re-entrant calls
    if (isEvaluating) return;
    setIsEvaluating(true);
    setLoading(true);
    setError(null);
    
    try {
      // First pass: evaluate independent formulas (no column variable references)
      // and identify dependent formulas
      let neededRows = rows;
      let neededColumns = columns;
      const columnData = [];
      const dependentCols = [];
      
      // First, check which columns have formulas with variable references
      for (let col = 0; col < columns; col++) {
        const formula = formulas[col];
        if (formula && formula.startsWith("=")) {
          const variables = extractColumnVariables(formula, headerNames);
          if (variables.length > 0) {
            dependentCols.push(col);
          }
        }
      }
      
      // Evaluate independent formulas first
      for (let col = 0; col < columns; col++) {
        if (dependentCols.includes(col)) continue;
        
        const formula = formulas[col];
        if (formula && formula.startsWith("=")) {
          try {
            const result = await evaluateFormula(formula, null, authToken);
            const values = extractValues(result);
            columnData[col] = values;
            
            // Update needed rows
            neededRows = Math.max(neededRows, values.length);
            
            // If values are objects, check if we need more columns for their fields
            if (values.length > 0 && values[0] && typeof values[0] === "object") {
              const fieldCount = Object.keys(values[0]).length;
              neededColumns = Math.max(neededColumns, col + fieldCount);
            }
          } catch (e) {
            setError(`Error evaluating formula in column ${col + 1}: ${e.message}`);
            columnData[col] = [];
          }
        }
      }
      
      // Update state if more rows or columns are needed
      if (neededRows > rows) {
        setRows(neededRows);
      }
      if (neededColumns > columns) {
        const newFormulas = [...formulas];
        const newHeaderNames = [...headerNames];
        const newColumnWidths = {...columnWidths};
        for (let c = columns; c < neededColumns; c++) {
          newFormulas[c] = null;
          newHeaderNames[c] = null;
          newColumnWidths[c] = undefined;
        }
        setColumns(neededColumns);
        setFormulas(newFormulas);
        setHeaderNames(newHeaderNames);
        setColumnWidths(newColumnWidths);
      }
      
      // Create initial data with correct dimensions
      const newData = Array(neededRows).fill(null).map(() => Array(neededColumns).fill(null));
      
      // Populate independent columns
      for (let col = 0; col < Math.min(columns, neededColumns); col++) {
        if (dependentCols.includes(col)) continue;
        
        const values = columnData[col];
        if (values) {
          // If values are objects, spread fields across columns
          if (values.length > 0 && values[0] && typeof values[0] === "object") {
            const fields = Object.keys(values[0]);
            for (let row = 0; row < Math.min(values.length, neededRows); row++) {
              const obj = values[row];
              for (let f = 0; f < fields.length; f++) {
                if (col + f < neededColumns) {
                  newData[row][col + f] = obj[fields[f]];
                }
              }
            }
          } else {
            // Simple values - populate down the column
            for (let row = 0; row < Math.min(values.length, neededRows); row++) {
              newData[row][col] = values[row];
            }
          }
        }
      }
      
      // Second pass: evaluate dependent formulas row-by-row
      for (let col of dependentCols) {
        if (col >= neededColumns) continue;
        
        const formula = formulas[col];
        if (formula && formula.startsWith("=")) {
          try {
            // Evaluate this formula for each row
            for (let row = 0; row < neededRows; row++) {
              const rowData = newData[row] || [];
              const result = await evaluateFormulaWithContext(formula, rowData, headerNames, authToken);
              
              if (result !== null && result !== undefined) {
                const values = extractValues(result);
                // For dependent formulas, we expect a single value per row
                if (Array.isArray(values) && values.length > 0) {
                  newData[row][col] = values[0];
                } else if (values !== null && values !== undefined) {
                  newData[row][col] = values;
                }
              }
            }
          } catch (e) {
            setError(`Error evaluating dependent formula in column ${col + 1}: ${e.message}`);
          }
        }
      }
      
      setData(newData);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
      setIsEvaluating(false);
    }
  }, [formulas, columns, rows, authToken, headerNames]);
  
  // Load table state on mount, then evaluate formulas
  useEffect(() => {
    loadTableState();
  }, []);
  
  // After table state is loaded, evaluate formulas
  useEffect(() => {
    if (tableStateLoaded) {
      evaluateAllFormulas();
    }
  }, [tableStateLoaded]);
  
  // Save state when formulas, header names, filters, sorting, or column order changes
  useEffect(() => {
    if (tableStateLoaded) {
      saveTableState();
    }
  }, [formulas, headerNames, columns, rows, filters, sortColumn, sortDirection, saveTableState, tableStateLoaded]);
  
  const handleHeaderNameChange = (colIndex, newName) => {
    const newHeaderNames = [...headerNames];
    newHeaderNames[colIndex] = newName;
    setHeaderNames(newHeaderNames);
  };
  
  const handleFormulaChange = (colIndex, newFormula) => {
    const newFormulas = [...formulas];
    newFormulas[colIndex] = newFormula;
    setFormulas(newFormulas);
    evaluateAllFormulas();
  };
  
  const handleColumnResize = (colIndex, width) => {
    setColumnWidths(prev => ({
      ...prev,
      [colIndex]: width
    }));
  };
  
  const handleFreezeColumn = () => {
    setFreezeFirstColumn(prev => !prev);
  };
  
  // Sorting handlers
  const handleSort = (colIndex) => {
    if (sortColumn === colIndex) {
      // Toggle direction or clear sort
      if (sortDirection === 'asc') {
        setSortDirection('desc');
      } else if (sortDirection === 'desc') {
        setSortColumn(null);
        setSortDirection(null);
      } else {
        setSortDirection('asc');
      }
    } else {
      // New column - sort ascending
      setSortColumn(colIndex);
      setSortDirection('asc');
    }
  };
  
  // Filtering handlers
  const handleFilterChange = (colIndex, value) => {
    setFilters(prev => ({
      ...prev,
      [colIndex]: value
    }));
  };
  
  const handleFilterClear = (colIndex) => {
    setFilters(prev => {
      const newFilters = {...prev};
      delete newFilters[colIndex];
      return newFilters;
    });
  };
  
  // Drag and drop handlers for column reordering
  const handleDragStart = (colIndex) => {
    setDraggedColumn(colIndex);
  };
  
  const handleDragOver = (e, colIndex) => {
    e.preventDefault();
    setDragOverColumn(colIndex);
  };
  
  const handleDragLeave = () => {
    setDragOverColumn(null);
  };
  
  const handleDragEnd = () => {
    setDraggedColumn(null);
    setDragOverColumn(null);
  };
  
  const handleDrop = (e, targetColIndex) => {
    e.preventDefault();
    setDragOverColumn(null);
    
    if (draggedColumn === null || draggedColumn === targetColIndex) {
      setDraggedColumn(null);
      return;
    }
    
    // Reorder columns
    const newColumnOrder = Array.from({ length: columns }, (_, i) => i);
    
    // Remove dragged column from its position
    newColumnOrder.splice(draggedColumn, 1);
    
    // Insert it at the target position
    newColumnOrder.splice(targetColIndex, 0, draggedColumn);
    
    // Reorder all column-related state
    const reorder = (array) => {
      return newColumnOrder.map(index => array[index]);
    };
    
    setFormulas(reorder(formulas));
    setHeaderNames(reorder(headerNames));
    setColumnWidths(prev => {
      const newWidths = {};
      Object.entries(prev).forEach(([oldIndex, width]) => {
        const newIndex = newColumnOrder.indexOf(parseInt(oldIndex, 10));
        if (newIndex !== -1) {
          newWidths[newIndex] = width;
        }
      });
      return newWidths;
    });
    
    // Reorder data rows
    setData(prev => prev.map(row => {
      const newRow = new Array(columns).fill(null);
      newColumnOrder.forEach((oldIndex, newIndex) => {
        newRow[newIndex] = row[oldIndex];
      });
      return newRow;
    }));
    
    // Also update sort state if it references a moved column
    if (sortColumn !== null) {
      const newSortColumn = newColumnOrder.indexOf(sortColumn);
      setSortColumn(newSortColumn);
    }
    
    setDraggedColumn(null);
  };
  
  const addColumn = () => {
    setColumns(columns + 1);
    setFormulas([...formulas, null]);
    setHeaderNames([...headerNames, null]);
    setData(data.map(row => [...row, null]));
  };
  
  const removeColumn = () => {
    if (columns > 1) {
      setColumns(columns - 1);
      setFormulas(formulas.slice(0, -1));
      setHeaderNames(headerNames.slice(0, -1));
      setData(data.map(row => row.slice(0, -1)));
    }
  };
  
  const addRow = () => {
    setRows(rows + 1);
    setData([...data, Array(columns).fill(null)]);
  };
  
  const removeRow = () => {
    if (rows > 1) {
      setRows(rows - 1);
      setData(data.slice(0, -1));
    }
  };
  
  return (
    <div className="programmable-table-container">
      <div className="toolbar">
        <button onClick={addColumn}>Add Column</button>
        <button onClick={removeColumn} disabled={columns <= 1}>Remove Column</button>
        <button onClick={addRow}>Add Row</button>
        <button onClick={removeRow} disabled={rows <= 1}>Remove Row</button>
        <button onClick={evaluateAllFormulas} disabled={loading}>
          {loading ? "Refreshing..." : "Refresh"}
        </button>
        {error && <span className="status error-status">{error}</span>}
        {loading && <span className="status">Loading...</span>}
        {savingState && <span className="status">Saving state...</span>}
        {stateError && <span className="status error-status">{stateError}</span>}
      </div>
      
      <div className="table-container">
        <table className="programmable-table">
          <thead className="frozen-header">
            <tr>
              {Array.from({ length: columns }).map((_, col) => (
                <HeaderCell
                  key={`header-${col}`}
                  value={formulas[col]}
                  headerName={headerNames[col]}
                  onHeaderNameChange={(newName) => handleHeaderNameChange(col, newName)}
                  onFormulaChange={(newFormula) => handleFormulaChange(col, newFormula)}
                  isFirstRow={true}
                  colIndex={col}
                  onResize={handleColumnResize}
                  width={columnWidths[col]}
                  onFreezeColumn={col === 0 ? handleFreezeColumn : null}
                  isFrozen={col === 0 && freezeFirstColumn}
                  onSort={handleSort}
                  sortColumn={sortColumn}
                  sortDirection={sortDirection}
                  onDragStart={handleDragStart}
                  onDragOver={handleDragOver}
                  onDragLeave={handleDragLeave}
                  onDrop={handleDrop}
                  onDragEnd={handleDragEnd}
                  draggedColumn={draggedColumn}
                  dragOverColumn={dragOverColumn}
                  onFilterChange={handleFilterChange}
                  onFilterClear={handleFilterClear}
                  filters={filters}
                />
              ))}
            </tr>
          </thead>
          <tbody>
            {(() => {
              // Apply filtering first, then sorting
              let displayData = getFilteredData(data, filters);
              displayData = getSortedData(displayData, sortColumn, sortDirection);
              
              return displayData.map((rowData, row) => (
                <tr key={`row-${row}`}>
                  {Array.from({ length: columns }).map((_, col) => (
                    <td 
                      key={`cell-${row}-${col}`} 
                      className={`table-cell ${col === 0 && freezeFirstColumn ? 'frozen-cell' : ''}`}
                      style={columnWidths[col] ? { width: `${columnWidths[col]}px` } : {}}
                    >
                      {rowData[col] !== null && rowData[col] !== undefined ? String(rowData[col]) : ""}
                    </td>
                  ))}
                </tr>
              ));
            })()}
          </tbody>
        </table>
      </div>
      
      <div className="formula-help">
        <p><strong>How to use:</strong> Right-click on any header to edit the header name, formula, or set filters. Click on headers to sort (asc/desc).</p>
        <p><strong>Configuration:</strong> Set <code>tableId</code> and <code>settingsEndpoint</code> to persist table state (formulas, header names, column widths).</p>
        <p><strong>Formula Examples:</strong></p>
        <ul>
          <li><code>=FETCH("http://localhost:8081/api/positions")</code> - Fetch all data</li>
          <li><code>=FETCH("http://localhost:8081/api/positions").code</code> - Extract 'code' field</li>
          <li><code>=FETCH("http://localhost:8081/api/positions").market</code> - Extract 'market' field</li>
          <li><code>=FETCH("http://localhost:8081/api/positions").map(p =&gt; p.market + ":" + p.code)</code> - Transform with JavaScript</li>
          <li><code>=FETCH("http://localhost:8081/api/positions").filter(p =&gt; p.price &gt; 100)</code> - Filter with JavaScript</li>
          <li><code>=FETCH("http://localhost:8081/api/positions/"+asxCode).size</code> - Reference column by header name (e.g., use value from column with header &quot;asxCode&quot; in same row)</li>
        </ul>
        <p><strong>Note:</strong> JavaScript expressions support <code>map</code>, <code>filter</code>, <code>reduce</code>, and other array methods.</p>
        <p><strong>Column References:</strong> Use column header names as variables in formulas. Formula is evaluated for each row using values from other columns in that row.</p>
        <p><strong>Sorting:</strong> Click column headers to sort (asc/desc/clear).</p>
        <p><strong>Filtering:</strong> Right-click on a header and use the filter input in the context menu to search within that column.</p>
        <p><strong>Drag & Drop:</strong> Drag column headers to reorder columns.</p>
        {savingState && <p className="status">Saving state...</p>}
        {stateError && <p className="status error-status">Error: {stateError}</p>}
      </div>
    </div>
  );
};

window.ProgrammableTable = ProgrammableTable;
