import { IconGitCommit } from '@tabler/icons-react'
import { shortSha, timeAgo } from '../../utils/format.js'
import WorkflowRow from './WorkflowRow.jsx'

export default function RunSet({ runs, isLatest }) {
  const firstRun = runs[0]

  return (
    <div
      style={{
        background: 'var(--color-bg-card)',
        border: '0.5px solid var(--color-border)',
        borderRadius: '12px',
        overflow: 'hidden',
        marginBottom: '10px',
      }}
    >
      <div
        style={{
          padding: '10px 1.25rem',
          background: 'var(--color-bg-repo-header)',
          borderBottom: '0.5px solid var(--color-border-repo-header)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <IconGitCommit size={14} color="var(--color-text-primary)" />
          <span style={{ fontSize: '12px', fontWeight: '500', color: 'var(--color-text-primary)' }}>
            {shortSha(firstRun.headSha)}
          </span>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          {isLatest && (
            <span
              style={{
                padding: '2px 8px',
                borderRadius: '20px',
                fontSize: '11px',
                background: 'var(--color-accent-bg)',
                color: 'var(--color-accent-text)',
                border: '0.5px solid var(--color-accent)',
              }}
            >
              Latest
            </span>
          )}
          <span style={{ fontSize: '12px', color: 'var(--color-text-secondary)' }}>
            {timeAgo(firstRun.startedAt)}
          </span>
        </div>
      </div>

      {runs.map((run, index) => (
        <div key={run.id} style={{ borderTop: index === 0 ? 'none' : undefined }}>
          <WorkflowRow run={run} />
        </div>
      ))}
    </div>
  )
}
