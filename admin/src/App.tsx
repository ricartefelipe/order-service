import { useState } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { CreatePage, LoginPage, LookupPage, OrderDetailPage, OrdersPage } from './pages'

export default function App() {
  const [authenticated, setAuthenticated] = useState(
    () => sessionStorage.getItem('order-ledger-totalrecall-session') !== null,
  )

  if (!authenticated) {
    return <LoginPage onAuthenticated={() => setAuthenticated(true)} />
  }

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<OrdersPage />} />
        <Route path="/new" element={<CreatePage />} />
        <Route path="/lookup" element={<LookupPage />} />
        <Route path="/orders/:id" element={<OrderDetailPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
