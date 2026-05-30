import { useParams, Link } from 'react-router-dom'
import { useState } from 'react'
import { useMainBranchRunSets } from '../api/runs.js'
import { useRepoPullRequests } from '../api/pullRequests.js'
import PageHeader from '../components/ui/PageHeader.jsx'
import Tabs from '../components/ui/Tabs.jsx'
import RepoRunsTab from '../components/run/RepoRunsTab.jsx'
import RepoPrsTab from '../components/pr/RepoPrsTab.jsx'

const TABS = ['Runs', 'Pull requests']

export default function RepoView() {
  const { owner, repo } = useParams()
  const [activeTab, setActiveTab] = useState('Runs')
  const [runsPage, setRunsPage] = useState(0)
  const [prStatus, setPrStatus] = useState('open')
  const [prPage, setPrPage] = useState(0)

  const { data: runSets, isLoading: runsLoading } = useMainBranchRunSets(owner, repo, runsPage)
  const { data: prs, isLoading: prsLoading } = useRepoPullRequests(owner, repo, prStatus, prPage)

  const sets = runSets?.content ?? []
  const totalRunPages = runSets?.totalPages ?? 1
  const prList = prs?.content ?? []
  const totalPrPages = prs?.totalPages ?? 1

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <PageHeader
        title={repo}
        subtitle={
          <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <Link
              to={`/${owner}/${repo}`}
              style={{
                color: 'var(--color-text-secondary)',
                textDecoration: 'underline',
                textUnderlineOffset: '2px',
              }}
            >
              {owner}
            </Link>
            <span style={{ color: 'var(--color-text-muted)' }}>· main branch</span>
          </span>
        }
      />

      <div style={{ padding: '1.5rem', overflowY: 'auto', flex: 1 }}>
        <Tabs tabs={TABS} activeTab={activeTab} onTabChange={setActiveTab} />

        {activeTab === 'Runs' && (
          <RepoRunsTab
            sets={sets}
            isLoading={runsLoading}
            page={runsPage}
            totalPages={totalRunPages}
            onPrevious={() => setRunsPage(p => p - 1)}
            onNext={() => setRunsPage(p => p + 1)}
          />
        )}

        {activeTab === 'Pull requests' && (
          <RepoPrsTab
            prs={prList}
            isLoading={prsLoading}
            status={prStatus}
            page={prPage}
            totalPages={totalPrPages}
            onStatusChange={s => { setPrStatus(s); setPrPage(0) }}
            onPrevious={() => setPrPage(p => p - 1)}
            onNext={() => setPrPage(p => p + 1)}
          />
        )}
      </div>
    </div>
  )
}