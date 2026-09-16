import { BrowserRouter, Route, Routes } from "react-router";
import { AdminGate } from "@/components/AdminGate";
import { AdminSongEditPage } from "@/pages/AdminSongEditPage";
import { AdminSongNewPage } from "@/pages/AdminSongNewPage";
import { AdminVideosPage } from "@/pages/AdminVideosPage";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/"
          element={
            <AdminGate>
              <AdminVideosPage />
            </AdminGate>
          }
        />
        <Route
          path="/admin/songs/new"
          element={
            <AdminGate>
              <AdminSongNewPage />
            </AdminGate>
          }
        />
        <Route
          path="/admin/songs/:id"
          element={
            <AdminGate>
              <AdminSongEditPage />
            </AdminGate>
          }
        />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
