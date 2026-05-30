import { useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { useCluster } from '../api/clusters.js'
import PageHeader from '../components/ui/PageHeader.jsx'
import OccurrenceRow from '../components/cluster/OccurrenceRow.jsx'
import Pagination from '../components/ui/Pagination.jsx'
import { timeAgo } from '../utils/format.js'

function representativeError(message) {
  if (!message) return ''
  const lines = message.split('\n')
  return lines.findLast((l) => l.includes('error')) ?? lines[0]
}

export default function ClusterDetail() {
  const { id } = useParams()
  const [page, setPage] = useState(0)
  const { data, isLoading, isError } = useCluster(id, page)

  if (isLoading) {
    return (
      <div style={{ padding: '2rem', fontSize: '14px', color: 'var(--color-text-secondary)' }}>
        Loading...
      </div>
    )
  }

  if (isError || !data) {
    return (
      <div style={{ padding: '2rem', fontSize: '14px', color: 'var(--color-fail-text)' }}>
        Something went wrong loading this cluster.
      </div>
    )
  }

  const { cluster, occurrences } = data

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <PageHeader
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            {cluster.jobName} · {cluster.stepName}
            <span
              style={{
                padding: '2px 8px',
                borderRadius: '20px',
                fontSize: '11px',
                fontWeight: '500',
                background: 'var(--color-fail-bg)',
                color: 'var(--color-fail-text)',
              }}
            >
              Failed {cluster.occurrenceCount}×
            </span>
          </div>
        }
        subtitle={
          <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <Link
              to={`/${cluster.owner}/${cluster.repo}`}
              style={{
                color: 'var(--color-text-secondary)',
                textDecoration: 'underline',
                textUnderlineOffset: '2px',
              }}
            >
              {cluster.owner}/{cluster.repo}
            </Link>
            <span style={{ color: 'var(--color-text-muted)' }}>
              · First seen {timeAgo(cluster.firstSeenAt)} · Last seen {timeAgo(cluster.lastSeenAt)}
            </span>
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
          Representative Error
        </p>

        <div
          style={{
            background: 'var(--color-bg-card)',
            border: '0.5px solid var(--color-border)',
            borderRadius: '12px',
            padding: '1rem 1.25rem',
            marginBottom: '1.5rem',
          }}
        >
          <div style={{ display: 'flex', gap: '2rem', flexWrap: 'wrap', marginBottom: '12px' }}>
            <div>
              <p
                style={{
                  fontSize: '11px',
                  color: 'var(--color-text-secondary)',
                  margin: '0 0 2px',
                }}
              >
                Job
              </p>
              <p
                style={{
                  fontSize: '13px',
                  fontWeight: '500',
                  color: 'var(--color-text-primary)',
                  margin: 0,
                }}
              >
                {cluster.jobName}
              </p>
            </div>
            <div>
              <p
                style={{
                  fontSize: '11px',
                  color: 'var(--color-text-secondary)',
                  margin: '0 0 2px',
                }}
              >
                Step
              </p>
              <p
                style={{
                  fontSize: '13px',
                  fontWeight: '500',
                  color: 'var(--color-text-primary)',
                  margin: 0,
                }}
              >
                {cluster.stepName}
              </p>
            </div>
            <div>
              <p
                style={{
                  fontSize: '11px',
                  color: 'var(--color-text-secondary)',
                  margin: '0 0 2px',
                }}
              >
                Occurrences
              </p>
              <p
                style={{
                  fontSize: '13px',
                  fontWeight: '500',
                  color: 'var(--color-text-primary)',
                  margin: 0,
                }}
              >
                {cluster.occurrenceCount}
              </p>
            </div>
            <div>
              <p
                style={{
                  fontSize: '11px',
                  color: 'var(--color-text-secondary)',
                  margin: '0 0 2px',
                }}
              >
                First seen
              </p>
              <p
                style={{
                  fontSize: '13px',
                  fontWeight: '500',
                  color: 'var(--color-text-primary)',
                  margin: 0,
                }}
              >
                {timeAgo(cluster.firstSeenAt)}
              </p>
            </div>
            <div>
              <p
                style={{
                  fontSize: '11px',
                  color: 'var(--color-text-secondary)',
                  margin: '0 0 2px',
                }}
              >
                Last seen
              </p>
              <p
                style={{
                  fontSize: '13px',
                  fontWeight: '500',
                  color: 'var(--color-text-primary)',
                  margin: 0,
                }}
              >
                {timeAgo(cluster.lastSeenAt)}
              </p>
            </div>
          </div>

          <div
            style={{
              background: 'var(--color-fail-bg)',
              border: '0.5px solid var(--color-fail-bar)',
              borderRadius: '8px',
              padding: '8px 10px',
            }}
          >
            <p
              style={{
                fontSize: '10px',
                fontWeight: '500',
                color: 'var(--color-fail-text)',
                margin: '0 0 4px',
                textTransform: 'uppercase',
                letterSpacing: '0.05em',
              }}
            >
              Error
            </p>
            <p
              style={{
                fontSize: '11px',
                fontFamily: 'monospace',
                color: 'var(--color-fail-text)',
                margin: 0,
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-all',
              }}
            >
              {representativeError(cluster.representativeMessage)}
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
          Occurrences
        </p>

        {occurrences.content.map((occurrence, index) => (
          <OccurrenceRow
            key={occurrence.id}
            occurrence={occurrence}
            defaultExpanded={page === 0 && index === 0}
          />
        ))}

        <Pagination
          page={page}
          hasMore={page < occurrences.totalPages - 1}
          onPrevious={() => setPage((p) => p - 1)}
          onNext={() => setPage((p) => p + 1)}
        />
      </div>
    </div>
  )
}
