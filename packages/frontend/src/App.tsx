import { useEffect, useState } from "react";
import "./App.css";

function App() {
  const [message, setMessage] = useState("loading backend");

  useEffect(() => {
    fetch("/api/ping")
      .then((r) => r.text())
      .then(setMessage)
      .catch(() => setMessage("failed to load backend"));
  }, []);

  return (
    <div>
      <p>Backend says: {message}</p>
    </div>
  );
}

export default App;
