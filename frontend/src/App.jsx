import { Routes, Route } from 'react-router-dom'
import Dashboard from './pages/Dashboard.jsx'
import RunDetail from './pages/RunDetail.jsx'
import RepoView from './pages/RepoView.jsx'
import ClusterDetail from './pages/ClusterDetail.jsx'
import PullRequestDetail from './pages/PullRequestDetail.jsx'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Dashboard />} />
      <Route path="/runs/:id" element={<RunDetail />} />
      <Route path="/clusters/:id" element={<ClusterDetail />} />
      <Route path="/pull-requests/:id" element={<PullRequestDetail />} />
      <Route path="/:owner/:repo" element={<RepoView />} />
    </Routes>
  )
}