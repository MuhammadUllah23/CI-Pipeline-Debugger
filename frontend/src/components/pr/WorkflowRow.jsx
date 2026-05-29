import { useNavigate } from 'react-router-dom'
import { IconCheck, IconX } from '@tabler/icons-react'
import { formatDuration } from '../../utils/format.js'

function statusStyle(conclusion) {
  if (conclusion === 'FAILURE')
    return { background: 'var(--color-fail-bg)', color: 'var(--color-fail-text)' }
  if (conclusion === 'SUCCESS')
    return { background: 'var(--color-pass-bg)', color: 'var(--color-pass-text)' }
  return { background: 'var(--color-progress-bg)', color: 'var(--color-progress-text)' }
}

function accentColor(conclusion) {
  if (conclusion === 'FAILURE') return 'var(--color-fail-bar)'
  if (conclusion === 'SUCCESS') return 'var(--color-pass-bar)'
  return 'var(--color-progress-bar)'
}

function statusLabel(conclusion) {
  if (conclusion === 'FAILURE') return 'Failed'
  if (conclusion === 'SUCCESS') return 'Passed'
  return 'In progress'
}

export default function WorkflowRow({ run }) {
  const navigate = useNavigate()

  return (
    <div
      onClick={() => navigate(`/runs/${run.id}`)}
      style={{
        padding: '0.625rem 1.25rem',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        borderTop: '0.5px solid var(--color-border)',
        cursor: 'pointer',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
        <div
          style={{
            width: '3px',
            height: '28px',
            borderRadius: '2px',
            flexShrink: 0,
            background: accentColor(run.conclusion),
          }}
        />
        <div>
          <p
            style={{
              fontSize: '13px',
              fontWeight: '500',
              color: 'var(--color-text-primary)',
              margin: 0,
            }}
          >
            {run.workflowName}
          </p>
          <p style={{ fontSize: '11px', color: 'var(--color-text-secondary)', margin: '2px 0 0' }}>
            {formatDuration(run.totalDurationMs)}
          </p>
        </div>
      </div>

      <span
        style={{
          padding: '3px 10px',
          borderRadius: '20px',
          fontSize: '11px',
          fontWeight: '500',
          ...statusStyle(run.conclusion),
        }}
      >
        {run.conclusion === 'FAILURE' ? <IconX size={11} /> : <IconCheck size={11} />}{' '}
        {statusLabel(run.conclusion)}
      </span>
    </div>
  )
}
