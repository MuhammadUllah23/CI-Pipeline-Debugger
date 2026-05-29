import { useParams, Link } from 'react-router-dom'
import { useState } from 'react'
import { usePullRequest } from '../api/pullRequests.js'
import PageHeader from '../components/ui/PageHeader.jsx'
import RunSet from '../components/pr/RunSet.jsx'
import { IconGitBranch } from '@tabler/icons-react'

function groupByHeadSha(runs) {
  const groups = []
  const seen = new Map()

  for (const run of runs) {
    const key = run.headSha
    if (!seen.has(key)) {
      seen.set(key, [])
      groups.push(seen.get(key))
    }
    seen.get(key).push(run)
  }

  return groups
}

function statePillStyle(state) {
  if (state === 'OPEN')
    return { background: 'var(--color-accent-bg)', color: 'var(--color-accent-text)' }
  if (state === 'MERGED')
    return { background: 'var(--color-pass-bg)', color: 'var(--color-pass-text)' }
  return { background: 'var(--color-bg-card)', color: 'var(--color-text-secondary)' }
}

export default function PullRequestDetail() {
  const { id } = useParams()
  const [page, setPage] = useState(0)
  const { data: pr, isLoading, isError } = usePullRequest(id, page)

  if (isLoading) {
    return (
      <div style={{ padding: '2rem', fontSize: '14px', color: 'var(--color-text-secondary)' }}>
        Loading...
      </div>
    )
  }

  if (isError || !pr) {
    return (
      <div style={{ padding: '2rem', fontSize: '14px', color: 'var(--color-fail-text)' }}>
        Something went wrong loading this pull request.
      </div>
    )
  }

  const groups = groupByHeadSha(pr.runs)
  const latestGroup = groups[0] ?? []
  const allGroups = groups

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
        <div style={{ marginBottom: '1.5rem' }}>
          <RunSet runs={latestGroup} isLatest={true} />
        </div>

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
        {allGroups.map((group, index) => (
          <RunSet key={group[0].headSha} runs={group} isLatest={index === 0} />
        ))}

        {pr.runs.length === 20 && (
          <div style={{ display: 'flex', justifyContent: 'center', gap: '8px', marginTop: '1rem' }}>
            <button onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0}>
              Previous
            </button>
            <button onClick={() => setPage((p) => p + 1)}>Next</button>
          </div>
        )}
      </div>
    </div>
  )
}
