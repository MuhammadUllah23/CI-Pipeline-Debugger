import { useState } from 'react'
import { IconChevronUp, IconChevronDown } from '@tabler/icons-react'
import PrCard from './PrCard.jsx'

function repoHealth(prs) {
  return prs.some((pr) => pr.runs.some((r) => r.conclusion === 'FAILURE'))
}

function repoSummary(prs) {
  const failingCount = prs.filter((pr) => pr.runs.some((r) => r.conclusion === 'FAILURE')).length
  const count = `${prs.length} open ${prs.length === 1 ? 'PR' : 'PRs'}`
  return failingCount > 0 ? `${count} · ${failingCount} failing` : `${count} · all passing`
}

export default function RepoSection({ repo, prs }) {
  const [expanded, setExpanded] = useState(true)
  const failing = repoHealth(prs)

  return (
    <div style={{ marginBottom: '10px' }}>
      <div
        onClick={() => setExpanded((prev) => !prev)}
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '10px 14px',
          background: 'var(--color-bg-repo-header)',
          border: '0.5px solid var(--color-border-repo-header)',
          borderRadius: expanded ? '12px 12px 0 0' : '12px',
          borderBottom: expanded ? 'none' : '0.5px solid var(--color-border-repo-header)',
          cursor: 'pointer',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <div
            style={{
              width: '8px',
              height: '8px',
              borderRadius: '50%',
              flexShrink: 0,
              background: failing ? 'var(--color-fail-bar)' : 'var(--color-pass-bar)',
            }}
          />
          <span style={{ fontSize: '14px', fontWeight: '500', color: 'var(--color-text-primary)' }}>
            {repo}
          </span>
          <span style={{ fontSize: '12px', color: 'var(--color-text-secondary)' }}>
            {repoSummary(prs)}
          </span>
        </div>
        {expanded ? (
          <IconChevronUp size={16} color="var(--color-text-secondary)" />
        ) : (
          <IconChevronDown size={16} color="var(--color-text-secondary)" />
        )}
      </div>

      <div
        style={{
          display: 'grid',
          gridTemplateRows: expanded ? '1fr' : '0fr',
          transition: 'grid-template-rows 0.25s ease',
        }}
      >
        <div style={{ overflow: 'hidden' }}>
          {prs.map((pr, index) => (
            <div key={pr.id} style={{ borderTop: index === 0 ? 'none' : undefined }}>
              <PrCard pr={pr} />
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
