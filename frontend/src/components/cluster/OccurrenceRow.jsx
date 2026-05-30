import { useState } from 'react'
import { Link } from 'react-router-dom'
import { IconChevronUp, IconChevronDown } from '@tabler/icons-react'
import { shortSha, timeAgo } from '../../utils/format.js'

export default function OccurrenceRow({ occurrence, defaultExpanded }) {
  const [expanded, setExpanded] = useState(defaultExpanded)

  return (
    <div
      style={{
        background: 'var(--color-bg-card)',
        border: '0.5px solid var(--color-border)',
        borderRadius: '12px',
        overflow: 'hidden',
        marginBottom: '8px',
      }}
    >
      <div
        onClick={() => setExpanded((prev) => !prev)}
        style={{
          padding: '10px 1.25rem',
          background: 'var(--color-bg-repo-header)',
          borderBottom: expanded ? '0.5px solid var(--color-border-repo-header)' : 'none',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          cursor: 'pointer',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <span style={{ fontSize: '12px', fontWeight: '500', color: 'var(--color-text-primary)' }}>
            {occurrence.branch}
          </span>
          <span
            style={{
              fontSize: '12px',
              fontFamily: 'monospace',
              color: 'var(--color-text-secondary)',
            }}
          >
            {shortSha(occurrence.headSha)}
          </span>
          <span style={{ fontSize: '12px', color: 'var(--color-text-secondary)' }}>
            {timeAgo(occurrence.createdAt)}
          </span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <Link
            to={`/runs/${occurrence.pipelineRunId}`}
            onClick={(e) => e.stopPropagation()}
            style={{
              fontSize: '12px',
              fontWeight: '500',
              color: 'var(--color-accent)',
              textDecoration: 'underline',
              textUnderlineOffset: '2px',
            }}
          >
            View run →
          </Link>
          {expanded ? (
            <IconChevronUp size={14} color="var(--color-text-secondary)" />
          ) : (
            <IconChevronDown size={14} color="var(--color-text-secondary)" />
          )}
        </div>
      </div>

      <div
        style={{
          display: 'grid',
          gridTemplateRows: expanded ? '1fr' : '0fr',
          transition: 'grid-template-rows 0.25s ease',
        }}
      >
        <div style={{ overflow: 'hidden' }}>
          {occurrence.snippet && (
            <div style={{ padding: '10px 1.25rem' }}>
              <p
                style={{
                  fontSize: '11px',
                  fontFamily: 'monospace',
                  color: 'var(--color-fail-text)',
                  margin: 0,
                  whiteSpace: 'pre-wrap',
                  wordBreak: 'break-all',
                  background: 'var(--color-fail-bg)',
                  border: '0.5px solid var(--color-fail-bar)',
                  borderRadius: '8px',
                  padding: '8px 10px',
                }}
              >
                {occurrence.snippet}
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
