import { useNavigate } from 'react-router-dom'
import { IconCheck, IconX, IconClock, IconGitBranch } from '@tabler/icons-react'

function conclusionIcon(conclusion) {
  if (conclusion === 'FAILURE') return <IconX size={11} />
  if (conclusion === 'SUCCESS') return <IconCheck size={11} />
  return <IconClock size={11} />
}

function chipStyle(conclusion) {
  if (conclusion === 'FAILURE')
    return {
      background: 'var(--color-fail-bg)',
      color: 'var(--color-fail-text)',
      borderColor: 'var(--color-fail-bar)',
    }
  if (conclusion === 'SUCCESS')
    return {
      background: 'var(--color-pass-bg)',
      color: 'var(--color-pass-text)',
      borderColor: 'var(--color-pass-bar)',
    }
  return {
    background: 'var(--color-progress-bg)',
    color: 'var(--color-progress-text)',
    borderColor: 'var(--color-progress-bar)',
  }
}

function isFailing(runs) {
  return runs.some((r) => r.conclusion === 'FAILURE')
}

export default function PrCard({ pr }) {
  const navigate = useNavigate()
  const failing = isFailing(pr.runs)

  return (
    <div
      onClick={() => navigate(`/pull-requests/${pr.id}`)}
      style={{
        padding: '0.75rem 1.25rem 0.75rem 1rem',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        borderTop: '0.5px solid var(--color-border)',
        background: 'var(--color-bg-card)',
        cursor: 'pointer',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: '10px', minWidth: 0 }}>
        <div
          style={{
            width: '3px',
            height: '32px',
            borderRadius: '2px',
            flexShrink: 0,
            background: failing ? 'var(--color-fail-bar)' : 'var(--color-pass-bar)',
          }}
        />
        <div style={{ minWidth: 0 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span style={{ fontSize: '12px', color: 'var(--color-text-secondary)', flexShrink: 0 }}>
              #{pr.prNumber}
            </span>
            <p
              style={{
                fontSize: '13px',
                fontWeight: '500',
                color: 'var(--color-text-primary)',
                margin: 0,
              }}
            >
              {pr.title}
            </p>
          </div>
          <p
            style={{
              fontSize: '11px',
              color: 'var(--color-text-secondary)',
              margin: '2px 0 0',
              display: 'flex',
              alignItems: 'center',
              gap: '3px',
            }}
          >
            <IconGitBranch size={11} />
            {pr.headBranch}
          </p>
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '6px', flexShrink: 0 }}>
        {pr.runs.map((run) => (
          <div
            key={run.id}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '4px',
              padding: '3px 8px',
              borderRadius: '6px',
              fontSize: '11px',
              border: '0.5px solid',
              ...chipStyle(run.conclusion),
            }}
          >
            {conclusionIcon(run.conclusion)}
            {run.workflowName}
          </div>
        ))}
      </div>
    </div>
  )
}
