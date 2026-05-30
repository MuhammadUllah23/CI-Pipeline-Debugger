import { IconCheck, IconX, IconClock } from '@tabler/icons-react'
import { formatDuration } from '../../utils/format.js'

function accentColor(conclusion) {
  if (conclusion === 'FAILURE') return 'var(--color-fail-bar)'
  if (conclusion === 'SUCCESS') return 'var(--color-pass-bar)'
  return 'var(--color-progress-bar)'
}

function statusStyle(conclusion) {
  if (conclusion === 'FAILURE')
    return { background: 'var(--color-fail-bg)', color: 'var(--color-fail-text)' }
  if (conclusion === 'SUCCESS')
    return { background: 'var(--color-pass-bg)', color: 'var(--color-pass-text)' }
  return { background: 'var(--color-progress-bg)', color: 'var(--color-progress-text)' }
}

function statusIcon(conclusion) {
  if (conclusion === 'FAILURE') return <IconX size={11} />
  if (conclusion === 'SUCCESS') return <IconCheck size={11} />
  return <IconClock size={11} />
}

function statusLabel(conclusion) {
  if (conclusion === 'FAILURE') return 'Failed'
  if (conclusion === 'SUCCESS') return 'Passed'
  return 'In progress'
}

export default function StepRow({ step }) {
  return (
    <>
      <div
        style={{
          padding: '0.5rem 1.25rem',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          borderTop: '0.5px solid var(--color-border)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <div
            style={{
              width: '3px',
              height: '24px',
              borderRadius: '2px',
              flexShrink: 0,
              background: accentColor(step.conclusion),
            }}
          />
          <span style={{ fontSize: '12px', color: 'var(--color-text-primary)' }}>
            {step.stepName}
          </span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span style={{ fontSize: '11px', color: 'var(--color-text-secondary)' }}>
            {formatDuration(step.durationMs)}
          </span>
          <span
            style={{
              padding: '2px 8px',
              borderRadius: '20px',
              fontSize: '11px',
              fontWeight: '500',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '4px',
              ...statusStyle(step.conclusion),
            }}
          >
            {statusIcon(step.conclusion)}
            {statusLabel(step.conclusion)}
          </span>
        </div>
      </div>

      {step.conclusion === 'FAILURE' &&
        (step.errorSnippet ? (
          <div
            style={{
              margin: '0 1.25rem 8px',
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
              {step.errorSnippet}
            </p>
          </div>
        ) : (
          <div style={{ margin: '0 1.25rem 8px', padding: '6px 10px' }}>
            <p
              style={{
                fontSize: '12px',
                color: 'var(--color-text-secondary)',
                fontStyle: 'italic',
                margin: 0,
              }}
            >
              Fetching error details...
            </p>
          </div>
        ))}
    </>
  )
}
