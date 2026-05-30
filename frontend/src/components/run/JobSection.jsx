import { useState } from 'react'
import { IconChevronUp, IconChevronDown } from '@tabler/icons-react'
import StepRow from './StepRow.jsx'

function hasFailed(steps) {
  return steps.some((s) => s.conclusion === 'FAILURE')
}

export default function JobSection({ jobName, steps }) {
  const [expanded, setExpanded] = useState(hasFailed(steps))

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
        <span style={{ fontSize: '12px', fontWeight: '500', color: 'var(--color-text-primary)' }}>
          {jobName}
        </span>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span style={{ fontSize: '12px', color: 'var(--color-text-secondary)' }}>
            {steps.length} {steps.length === 1 ? 'step' : 'steps'}
          </span>
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
          {steps.map((step) => (
            <StepRow key={step.id} step={step} />
          ))}
        </div>
      </div>
    </div>
  )
}
