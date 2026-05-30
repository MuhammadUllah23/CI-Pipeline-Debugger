import { useParams, Link } from 'react-router-dom'
import { useRun, useRunSteps, useRunClusters } from '../api/runs.js'
import PageHeader from '../components/ui/PageHeader.jsx'
import JobSection from '../components/run/JobSection.jsx'
import { shortSha, timeAgo, formatDuration } from '../utils/format.js'

function conclusionPill(conclusion) {
  if (conclusion === 'FAILURE')
    return { label: 'Failed', bg: 'var(--color-fail-bg)', color: 'var(--color-fail-text)' }
  if (conclusion === 'SUCCESS')
    return { label: 'Passed', bg: 'var(--color-pass-bg)', color: 'var(--color-pass-text)' }
  return {
    label: 'In progress',
    bg: 'var(--color-progress-bg)',
    color: 'var(--color-progress-text)',
  }
}

function groupByJob(steps) {
  const groups = new Map()
  for (const step of steps) {
    const job = step.jobName ?? 'unknown'
    if (!groups.has(job)) groups.set(job, [])
    groups.get(job).push(step)
  }
  return groups
}

function representativeError(message) {
  if (!message) return ''
  const lines = message.split('\n')
  return lines.findLast((l) => l.includes('error')) ?? lines[0]
}

export default function RunDetail() {
  const { id } = useParams()
  const { data: run, isLoading: runLoading, isError: runError } = useRun(id)
  const { data: steps, isLoading: stepsLoading } = useRunSteps(id)
  const { data: clusters } = useRunClusters(id, run?.conclusion)

  if (runLoading) {
    return (
      <div style={{ padding: '2rem', fontSize: '14px', color: 'var(--color-text-secondary)' }}>
        Loading...
      </div>
    )
  }

  if (runError || !run) {
    return (
      <div style={{ padding: '2rem', fontSize: '14px', color: 'var(--color-fail-text)' }}>
        Something went wrong loading this run.
      </div>
    )
  }

  const pill = conclusionPill(run.conclusion)
  const jobGroups = groupByJob(steps ?? [])

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <PageHeader
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            {run.workflowName}
            <span
              style={{
                padding: '2px 8px',
                borderRadius: '20px',
                fontSize: '11px',
                fontWeight: '500',
                background: pill.bg,
                color: pill.color,
              }}
            >
              {pill.label}
            </span>
          </div>
        }
        subtitle={
          <span style={{ display: 'flex', alignItems: 'center', gap: '6px', flexWrap: 'wrap' }}>
            <Link
              to={`/${run.owner}/${run.repo}`}
              style={{
                color: 'var(--color-text-secondary)',
                textDecoration: 'underline',
                textUnderlineOffset: '2px',
              }}
            >
              {run.owner}/{run.repo}
            </Link>
            {run.pullRequestId && (
              <>
                <span style={{ color: 'var(--color-text-muted)' }}>→</span>
                <Link
                  to={`/pull-requests/${run.pullRequestId}`}
                  style={{
                    color: 'var(--color-text-secondary)',
                    textDecoration: 'underline',
                    textUnderlineOffset: '2px',
                  }}
                >
                  PR #{run.prNumber}
                </Link>
              </>
            )}
            <span style={{ color: 'var(--color-text-muted)' }}>→</span>
            <span style={{ color: 'var(--color-text-primary)', fontWeight: '500' }}>Run</span>
          </span>
        }
      />

      <div style={{ padding: '1.5rem', overflowY: 'auto', flex: 1 }}>
        <div
          style={{
            background: 'var(--color-bg-card)',
            border: '0.5px solid var(--color-border)',
            borderRadius: '12px',
            padding: '0.875rem 1.25rem',
            display: 'flex',
            gap: '2rem',
            marginBottom: '1.5rem',
            flexWrap: 'wrap',
          }}
        >
          <div>
            <p
              style={{ fontSize: '11px', color: 'var(--color-text-secondary)', margin: '0 0 2px' }}
            >
              Branch
            </p>
            <p
              style={{
                fontSize: '13px',
                fontWeight: '500',
                color: 'var(--color-text-primary)',
                margin: 0,
              }}
            >
              {run.branch}
            </p>
          </div>
          <div>
            <p
              style={{ fontSize: '11px', color: 'var(--color-text-secondary)', margin: '0 0 2px' }}
            >
              Commit
            </p>
            <p
              style={{
                fontSize: '13px',
                fontWeight: '500',
                color: 'var(--color-text-primary)',
                margin: 0,
                fontFamily: 'monospace',
              }}
            >
              {shortSha(run.headSha)}
            </p>
          </div>
          <div>
            <p
              style={{ fontSize: '11px', color: 'var(--color-text-secondary)', margin: '0 0 2px' }}
            >
              Duration
            </p>
            <p
              style={{
                fontSize: '13px',
                fontWeight: '500',
                color: 'var(--color-text-primary)',
                margin: 0,
              }}
            >
              {formatDuration(run.totalDurationMs)}
            </p>
          </div>
          <div>
            <p
              style={{ fontSize: '11px', color: 'var(--color-text-secondary)', margin: '0 0 2px' }}
            >
              Started
            </p>
            <p
              style={{
                fontSize: '13px',
                fontWeight: '500',
                color: 'var(--color-text-primary)',
                margin: 0,
              }}
            >
              {timeAgo(run.startedAt)}
            </p>
          </div>
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
          Steps
        </p>

        {stepsLoading || (steps ?? []).length === 0 ? (
          <p style={{ fontSize: '14px', color: 'var(--color-text-secondary)' }}>
            {run.status === 'COMPLETED' && (steps ?? []).length === 0
              ? 'Fetching step data...'
              : 'Loading steps...'}
          </p>
        ) : (
          [...jobGroups.entries()].map(([jobName, jobSteps]) => (
            <JobSection key={jobName} jobName={jobName} steps={jobSteps} />
          ))
        )}

        {clusters && clusters.length > 0 && (
          <>
            <p
              style={{
                fontSize: '12px',
                color: 'var(--color-text-secondary)',
                textTransform: 'uppercase',
                letterSpacing: '0.06em',
                margin: '1.5rem 0 10px',
              }}
            >
              Error Clusters
            </p>
            {clusters.map((cluster) => (
              <Link
                key={cluster.id}
                to={`/clusters/${cluster.id}`}
                style={{ textDecoration: 'none' }}
              >
                <div
                  style={{
                    background: 'var(--color-bg-card)',
                    border: '0.5px solid var(--color-border)',
                    borderRadius: '12px',
                    padding: '0.75rem 1.25rem',
                    marginBottom: '8px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    cursor: 'pointer',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', minWidth: 0 }}>
                    <div
                      style={{
                        width: '3px',
                        height: '32px',
                        borderRadius: '2px',
                        background: 'var(--color-fail-bar)',
                        flexShrink: 0,
                      }}
                    />
                    <div style={{ minWidth: 0 }}>
                      <p
                        style={{
                          fontSize: '13px',
                          fontWeight: '500',
                          color: 'var(--color-text-primary)',
                          margin: '0 0 2px',
                        }}
                      >
                        {cluster.jobName} · {cluster.stepName}
                      </p>
                      <p
                        style={{
                          fontSize: '11px',
                          color: 'var(--color-text-secondary)',
                          margin: 0,
                          fontFamily: 'monospace',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap',
                        }}
                      >
                        {representativeError(cluster.representativeMessage)}
                      </p>
                    </div>
                  </div>
                  <span
                    style={{
                      padding: '2px 8px',
                      borderRadius: '20px',
                      fontSize: '11px',
                      fontWeight: '500',
                      background: 'var(--color-fail-bg)',
                      color: 'var(--color-fail-text)',
                      flexShrink: 0,
                      marginLeft: '12px',
                    }}
                  >
                    {cluster.occurrenceCount}×
                  </span>
                </div>
              </Link>
            ))}
          </>
        )}
      </div>
    </div>
  )
}
