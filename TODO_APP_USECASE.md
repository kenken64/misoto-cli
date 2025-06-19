I'll walk you through creating a complete todo application with React frontend, Node.js/Express backend, and SQLite database. Here are all the steps:

## Backend Setup (Node.js + Express + SQLite)

### 1. Initialize the backend project
```bash
mkdir todo-app-backend
cd todo-app-backend
npm init -y
```

### 2. Install backend dependencies
```bash
npm install express sqlite3 cors body-parser
npm install -D nodemon
```

### 3. Create the backend structure
```
todo-app-backend/
├── server.js
├── database.js
└── package.json
```

### 4. Create the database setup (database.js)

```
const sqlite3 = require('sqlite3').verbose();
const path = require('path');

// Create or connect to SQLite database
const dbPath = path.join(__dirname, 'todos.db');
const db = new sqlite3.Database(dbPath, (err) => {
  if (err) {
    console.error('Error opening database:', err.message);
  } else {
    console.log('Connected to SQLite database');
  }
});

// Create todos table if it doesn't exist
db.serialize(() => {
  db.run(`CREATE TABLE IF NOT EXISTS todos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    description TEXT,
    completed BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
  )`);
});

module.exports = db;
```

### 5. Create the Express server (server.js)

```
const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
const db = require('./database');

const app = express();
const PORT = process.env.PORT || 5000;

// Middleware
app.use(cors());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));

// GET all todos
app.get('/api/todos', (req, res) => {
  const sql = 'SELECT * FROM todos ORDER BY created_at DESC';
  
  db.all(sql, [], (err, rows) => {
    if (err) {
      res.status(500).json({ error: err.message });
      return;
    }
    res.json({
      message: 'success',
      data: rows
    });
  });
});

// GET single todo by ID
app.get('/api/todos/:id', (req, res) => {
  const sql = 'SELECT * FROM todos WHERE id = ?';
  const params = [req.params.id];
  
  db.get(sql, params, (err, row) => {
    if (err) {
      res.status(500).json({ error: err.message });
      return;
    }
    if (row) {
      res.json({
        message: 'success',
        data: row
      });
    } else {
      res.status(404).json({ message: 'Todo not found' });
    }
  });
});

// POST create new todo
app.post('/api/todos', (req, res) => {
  const { title, description } = req.body;
  
  if (!title) {
    return res.status(400).json({ error: 'Title is required' });
  }
  
  const sql = 'INSERT INTO todos (title, description) VALUES (?, ?)';
  const params = [title, description || ''];
  
  db.run(sql, params, function(err) {
    if (err) {
      res.status(500).json({ error: err.message });
      return;
    }
    res.json({
      message: 'Todo created successfully',
      data: {
        id: this.lastID,
        title,
        description: description || '',
        completed: false
      }
    });
  });
});

// PUT update todo
app.put('/api/todos/:id', (req, res) => {
  const { title, description, completed } = req.body;
  const sql = `UPDATE todos 
               SET title = ?, description = ?, completed = ?, updated_at = CURRENT_TIMESTAMP 
               WHERE id = ?`;
  const params = [title, description, completed, req.params.id];
  
  db.run(sql, params, function(err) {
    if (err) {
      res.status500().json({ error: err.message });
      return;
    }
    if (this.changes === 0) {
      res.status(404).json({ message: 'Todo not found' });
    } else {
      res.json({
        message: 'Todo updated successfully',
        changes: this.changes
      });
    }
  });
});

// PATCH toggle todo completion status
app.patch('/api/todos/:id/toggle', (req, res) => {
  const sql = `UPDATE todos 
               SET completed = NOT completed, updated_at = CURRENT_TIMESTAMP 
               WHERE id = ?`;
  const params = [req.params.id];
  
  db.run(sql, params, function(err) {
    if (err) {
      res.status(500).json({ error: err.message });
      return;
    }
    if (this.changes === 0) {
      res.status(404).json({ message: 'Todo not found' });
    } else {
      res.json({
        message: 'Todo status toggled successfully',
        changes: this.changes
      });
    }
  });
});

// DELETE todo
app.delete('/api/todos/:id', (req, res) => {
  const sql = 'DELETE FROM todos WHERE id = ?';
  const params = [req.params.id];
  
  db.run(sql, params, function(err) {
    if (err) {
      res.status(500).json({ error: err.message });
      return;
    }
    if (this.changes === 0) {
      res.status(404).json({ message: 'Todo not found' });
    } else {
      res.json({
        message: 'Todo deleted successfully',
        changes: this.changes
      });
    }
  });
});

// Health check endpoint
app.get('/api/health', (req, res) => {
  res.json({ message: 'Server is running!' });
});

// Start server
app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
});

// Graceful shutdown
process.on('SIGINT', () => {
  db.close((err) => {
    if (err) {
      console.error(err.message);
    }
    console.log('Database connection closed.');
    process.exit(0);
  });
});
```

### 6	. Update package.json scripts
```json
{
  "scripts": {
    "start": "node server.js",
    "dev": "nodemon server.js"
  }
}
```

## Frontend Setup (React)

### 7. Create React app
```bash
# In a new terminal, go back to parent directory
cd ..
npx create-react-app todo-app-frontend
cd todo-app-frontend
```

### 8. Install additional frontend dependencies
```bash
npm install axios
```

### 9. Create the React components

```
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './App.css';

const API_BASE_URL = 'http://localhost:5000/api';

function App() {
  const [todos, setTodos] = useState([]);
  const [newTodo, setNewTodo] = useState({ title: '', description: '' });
  const [editingTodo, setEditingTodo] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Fetch all todos
  const fetchTodos = async () => {
    try {
      setLoading(true);
      const response = await axios.get(`${API_BASE_URL}/todos`);
      setTodos(response.data.data);
      setError('');
    } catch (err) {
      setError('Failed to fetch todos');
      console.error('Error fetching todos:', err);
    } finally {
      setLoading(false);
    }
  };

  // Create new todo
  const createTodo = async (e) => {
    e.preventDefault();
    if (!newTodo.title.trim()) return;

    try {
      setLoading(true);
      const response = await axios.post(`${API_BASE_URL}/todos`, newTodo);
      setTodos([response.data.data, ...todos]);
      setNewTodo({ title: '', description: '' });
      setError('');
    } catch (err) {
      setError('Failed to create todo');
      console.error('Error creating todo:', err);
    } finally {
      setLoading(false);
    }
  };

  // Update todo
  const updateTodo = async (id, updatedTodo) => {
    try {
      setLoading(true);
      await axios.put(`${API_BASE_URL}/todos/${id}`, updatedTodo);
      setTodos(todos.map(todo => 
        todo.id === id ? { ...todo, ...updatedTodo } : todo
      ));
      setEditingTodo(null);
      setError('');
    } catch (err) {
      setError('Failed to update todo');
      console.error('Error updating todo:', err);
    } finally {
      setLoading(false);
    }
  };

  // Toggle todo completion
  const toggleTodo = async (id) => {
    try {
      await axios.patch(`${API_BASE_URL}/todos/${id}/toggle`);
      setTodos(todos.map(todo => 
        todo.id === id ? { ...todo, completed: !todo.completed } : todo
      ));
      setError('');
    } catch (err) {
      setError('Failed to toggle todo');
      console.error('Error toggling todo:', err);
    }
  };

  // Delete todo
  const deleteTodo = async (id) => {
    if (window.confirm('Are you sure you want to delete this todo?')) {
      try {
        setLoading(true);
        await axios.delete(`${API_BASE_URL}/todos/${id}`);
        setTodos(todos.filter(todo => todo.id !== id));
        setError('');
      } catch (err) {
        setError('Failed to delete todo');
        console.error('Error deleting todo:', err);
      } finally {
        setLoading(false);
      }
    }
  };

  // Load todos on component mount
  useEffect(() => {
    fetchTodos();
  }, []);

  return (
    <div className="App">
      <div className="container">
        <h1>Todo Application</h1>
        
        {error && <div className="error-message">{error}</div>}
        
        {/* Add new todo form */}
        <form onSubmit={createTodo} className="todo-form">
          <div className="form-group">
            <input
              type="text"
              placeholder="Todo title..."
              value={newTodo.title}
              onChange={(e) => setNewTodo({...newTodo, title: e.target.value})}
              disabled={loading}
            />
          </div>
          <div className="form-group">
            <textarea
              placeholder="Description (optional)..."
              value={newTodo.description}
              onChange={(e) => setNewTodo({...newTodo, description: e.target.value})}
              disabled={loading}
            />
          </div>
          <button type="submit" disabled={loading || !newTodo.title.trim()}>
            {loading ? 'Adding...' : 'Add Todo'}
          </button>
        </form>

        {/* Todo list */}
        <div className="todo-list">
          {loading && todos.length === 0 ? (
            <div className="loading">Loading todos...</div>
          ) : todos.length === 0 ? (
            <div className="empty-state">No todos yet. Add one above!</div>
          ) : (
            todos.map(todo => (
              <div key={todo.id} className={`todo-item ${todo.completed ? 'completed' : ''}`}>
                {editingTodo === todo.id ? (
                  <EditTodoForm
                    todo={todo}
                    onSave={(updatedTodo) => updateTodo(todo.id, updatedTodo)}
                    onCancel={() => setEditingTodo(null)}
                    loading={loading}
                  />
                ) : (
                  <TodoItem
                    todo={todo}
                    onToggle={() => toggleTodo(todo.id)}
                    onEdit={() => setEditingTodo(todo.id)}
                    onDelete={() => deleteTodo(todo.id)}
                  />
                )}
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}

// Todo item component
function TodoItem({ todo, onToggle, onEdit, onDelete }) {
  return (
    <>
      <div className="todo-content">
        <input
          type="checkbox"
          checked={todo.completed}
          onChange={onToggle}
          className="todo-checkbox"
        />
        <div className="todo-text">
          <h3 className="todo-title">{todo.title}</h3>
          {todo.description && <p className="todo-description">{todo.description}</p>}
          <small className="todo-date">
            Created: {new Date(todo.created_at).toLocaleDateString()}
          </small>
        </div>
      </div>
      <div className="todo-actions">
        <button onClick={onEdit} className="edit-btn">Edit</button>
        <button onClick={onDelete} className="delete-btn">Delete</button>
      </div>
    </>
  );
}

// Edit todo form component
function EditTodoForm({ todo, onSave, onCancel, loading }) {
  const [editedTodo, setEditedTodo] = useState({
    title: todo.title,
    description: todo.description || '',
    completed: todo.completed
  });

  const handleSave = (e) => {
    e.preventDefault();
    if (!editedTodo.title.trim()) return;
    onSave(editedTodo);
  };

  return (
    <form onSubmit={handleSave} className="edit-form">
      <div className="form-group">
        <input
          type="text"
          value={editedTodo.title}
          onChange={(e) => setEditedTodo({...editedTodo, title: e.target.value})}
          disabled={loading}
          autoFocus
        />
      </div>
      <div className="form-group">
        <textarea
          value={editedTodo.description}
          onChange={(e) => setEditedTodo({...editedTodo, description: e.target.value})}
          disabled={loading}
        />
      </div>
      <div className="form-actions">
        <button type="submit" disabled={loading || !editedTodo.title.trim()}>
          {loading ? 'Saving...' : 'Save'}
        </button>
        <button type="button" onClick={onCancel} disabled={loading}>
          Cancel
        </button>
      </div>
    </form>
  );
}

export default App;

```
### 10. Create CSS styles (App.css)

```
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen',
    'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue',
    sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  background-color: #f5f5f5;
}

.App {
  min-height: 100vh;
  padding: 20px;
}

.container {
  max-width: 800px;
  margin: 0 auto;
  background: white;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  padding: 30px;
}

h1 {
  text-align: center;
  color: #333;
  margin-bottom: 30px;
  font-size: 2.5rem;
}

.error-message {
  background-color: #fee;
  color: #c33;
  padding: 10px;
  border-radius: 5px;
  margin-bottom: 20px;
  border: 1px solid #fcc;
}

.todo-form {
  background: #f9f9f9;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 30px;
}

.form-group {
  margin-bottom: 15px;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 12px;
  border: 2px solid #ddd;
  border-radius: 5px;
  font-size: 16px;
  transition: border-color 0.3s;
}

.form-group input:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #007bff;
}

.form-group textarea {
  resize: vertical;
  min-height: 80px;
}

button {
  background-color: #007bff;
  color: white;
  border: none;
  padding: 12px 20px;
  border-radius: 5px;
  cursor: pointer;
  font-size: 16px;
  transition: background-color 0.3s;
}

button:hover:not(:disabled) {
  background-color: #0056b3;
}

button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.todo-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #666;
  font-size: 18px;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #999;
  font-size: 18px;
}

.todo-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 20px;
  border: 2px solid #eee;
  border-radius: 8px;
  background: white;
  transition: all 0.3s;
}

.todo-item:hover {
  border-color: #007bff;
  box-shadow: 0 2px 8px rgba(0, 123, 255, 0.15);
}

.todo-item.completed {
  background-color: #f8f9fa;
  border-color: #28a745;
}

.todo-item.completed .todo-title {
  text-decoration: line-through;
  color: #666;
}

.todo-content {
  display: flex;
  align-items: flex-start;
  gap: 15px;
  flex: 1;
}

.todo-checkbox {
  width: 20px;
  height: 20px;
  margin-top: 5px;
  cursor: pointer;
}

.todo-text {
  flex: 1;
}

.todo-title {
  margin-bottom: 8px;
  color: #333;
  font-size: 18px;
}

.todo-description {
  color: #666;
  margin-bottom: 8px;
  line-height: 1.5;
}

.todo-date {
  color: #999;
  font-size: 12px;
}

.todo-actions {
  display: flex;
  gap: 10px;
  margin-left: 15px;
}

.edit-btn {
  background-color: #ffc107;
  color: #333;
  padding: 8px 16px;
  font-size: 14px;
}

.edit-btn:hover {
  background-color: #e0a800;
}

.delete-btn {
  background-color: #dc3545;
  padding: 8px 16px;
  font-size: 14px;
}

.delete-btn:hover {
  background-color: #c82333;
}

.edit-form {
  flex: 1;
}

.form-actions {
  display: flex;
  gap: 10px;
  margin-top: 15px;
}

.form-actions button {
  padding: 8px 16px;
  font-size: 14px;
}

.form-actions button[type="button"] {
  background-color: #6c757d;
}

.form-actions button[type="button"]:hover {
  background-color: #545b62;
}

/* Responsive design */
@media (max-width: 600px) {
  .container {
    padding: 20px;
    margin: 0 10px;
  }
  
  h1 {
    font-size: 2rem;
  }
  
  .todo-item {
    flex-direction: column;
    gap: 15px;
  }
  
  .todo-actions {
    margin-left: 0;
    justify-content: flex-end;
  }
  
  .form-actions {
    flex-direction: column;
  }
}
```
## Running the Application

### 11. Start the backend server
```bash
# In the backend directory
cd todo-app-backend
npm run dev
```
The server will start on http://localhost:5000

### 12. Start the frontend development server
```bash
# In a new terminal, go to frontend directory
cd todo-app-frontend
npm start
```
The React app will start on http://localhost:3000

## Project Structure

Your final project structure should look like this:

```
todo-app/
├── todo-app-backend/
│   ├── server.js
│   ├── database.js
│   ├── package.json
│   └── todos.db (created automatically)
└── todo-app-frontend/
    ├── src/
    │   ├── App.js
    │   ├── App.css
    │   └── index.js
    ├── package.json
    └── public/
```

## API Endpoints

Your backend provides these endpoints:

- `GET /api/todos` - Get all todos
- `GET /api/todos/:id` - Get single todo
- `POST /api/todos` - Create new todo
- `PUT /api/todos/:id` - Update todo
- `PATCH /api/todos/:id/toggle` - Toggle completion status
- `DELETE /api/todos/:id` - Delete todo
- `GET /api/health` - Health check

## Features Included

- ✅ Create new todos with title and description
- ✅ View all todos in a clean interface
- ✅ Mark todos as complete/incomplete
- ✅ Edit existing todos inline
- ✅ Delete todos with confirmation
- ✅ Responsive design for mobile devices
- ✅ Error handling and loading states
- ✅ SQLite database persistence
- ✅ RESTful API design

The application is now fully functional with all CRUD operations working between the React frontend and Node.js/Express backend, with data persisted in SQLite database!
