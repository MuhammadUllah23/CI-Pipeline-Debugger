import { Route, Routes } from 'react-router-dom'
import Layout from './components/layout/Layout.jsx'
import ClusterDetail from './pages/ClusterDetail.jsx'
import Dashboard from './pages/Dashboard.jsx'
import PullRequestDetail from './pages/PullRequestDetail.jsx'
import RepoView from './pages/RepoView.jsx'
import RunDetail from './pages/RunDetail.jsx'

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Dashboard />} />
        <Route path="/runs/:id" element={<RunDetail />} />
        <Route path="/clusters/:id" element={<ClusterDetail />} />
        <Route path="/pull-requests/:id" element={<PullRequestDetail />} />
        <Route path="/:owner/:repo" element={<RepoView />} />
      </Route>
    </Routes>
  )
}
