import { useParams, Link } from 'react-router-dom'
import { useState } from 'react'
import { usePullRequest, useRunSets } from '../api/pullRequests.js'
import PageHeader from '../components/ui/PageHeader.jsx'
import RunSet from '../components/pr/RunSet.jsx'
import Pagination from '../components/ui/Pagination.jsx'
import { IconGitBranch } from '@tabler/icons-react'

function statePillStyle(state) {
  if (state === 'OPEN')
    return { background: 'var(--color-accent-bg)', color: 'var(--color-accent-text)' }
  if (state === 'MERGED')
    return { background: 'var(--color-pass-bg)', color: 'var(--color-pass-text)' }
  return { background: 'var(--color-bg-card)', color: 'var(--color-text-secondary)' }
}

function getLatestPerWorkflow(runs) {
  const seen = new Set()
  return runs.filter((run) => {
    if (seen.has(run.workflowName)) return false
    seen.add(run.workflowName)
    return true
  })
}

export default function PullRequestDetail() {
  const { id } = useParams()
  const [page, setPage] = useState(0)

  const { data: pr, isLoading: prLoading, isError: prError } = usePullRequest(id, 0)
  const { data: runSets, isLoading: setsLoading } = useRunSets(id, page)

  if (prLoading) {
    return (
      <div style={{ padding: '2rem', fontSize: '14px', color: 'var(--color-text-secondary)' }}>
        Loading...
      </div>
    )
  }

  if (prError || !pr) {
    return (
      <div style={{ padding: '2rem', fontSize: '14px', color: 'var(--color-fail-text)' }}>
        Something went wrong loading this pull request.
      </div>
    )
  }

  const latestRuns = getLatestPerWorkflow(pr.runs ?? [])
  const sets = runSets?.content ?? []
  const totalPages = runSets?.totalPages ?? 1

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <PageHeader
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '13px', color: 'var(--color-text-secondary)' }}>
              #{pr.prNumber}
            </span>
            {pr.title}
            <span
              style={{
                padding: '2px 8px',
                borderRadius: '20px',
                fontSize: '11px',
                fontWeight: '500',
                ...statePillStyle(pr.prState),
              }}
            >
              {pr.prState.charAt(0) + pr.prState.slice(1).toLowerCase()}
            </span>
          </div>
        }
        subtitle={
          <span style={{ display: 'flex', alignItems: 'center', gap: '6px', flexWrap: 'wrap' }}>
            <Link
              to={`/${pr.owner}/${pr.repo}`}
              style={{
                color: 'var(--color-text-secondary)',
                textDecoration: 'underline',
                textUnderlineOffset: '2px',
              }}
              onClick={(e) => e.stopPropagation()}
            >
              {pr.owner}/{pr.repo}
            </Link>
            <span style={{ color: 'var(--color-text-muted)' }}>→</span>
            <span style={{ color: 'var(--color-text-primary)', fontWeight: '500' }}>
              PR #{pr.prNumber} · {pr.title}
            </span>
            <span style={{ color: 'var(--color-text-muted)' }}>·</span>
            <IconGitBranch size={12} />
            {pr.headBranch}
          </span>
        }
      />

      <div style={{ padding: '1.5rem', overflowY: 'auto', flex: 1 }}>
        <p
          style={{
            fontSize: '12px',
            color: 'var(--color-text-secondary)',
            textTransform: 'uppercase',
            letterSpacing: '0.06em',
            margin: '0 0 10px',
          }}
        >
          Latest checks
        </p>
        {latestRuns.length > 0 && (
          <div style={{ marginBottom: '1.5rem' }}>
            <RunSet runs={latestRuns} isLatest={true} defaultExpanded={true} />
          </div>
        )}

        <p
          style={{
            fontSize: '12px',
            color: 'var(--color-text-secondary)',
            textTransform: 'uppercase',
            letterSpacing: '0.06em',
            margin: '0 0 10px',
          }}
        >
          Run history
        </p>

        {setsLoading ? (
          <p style={{ fontSize: '14px', color: 'var(--color-text-secondary)' }}>Loading...</p>
        ) : (
          sets.map((set, index) => (
            <RunSet
              key={set.headSha}
              runs={set.runs}
              isLatest={page === 0 && index === 0}
              defaultExpanded={false}
            />
          ))
        )}

        <Pagination
          page={page}
          hasMore={page < totalPages - 1}
          onPrevious={() => setPage((p) => p - 1)}
          onNext={() => setPage((p) => p + 1)}
        />
      </div>
    </div>
  )
}
