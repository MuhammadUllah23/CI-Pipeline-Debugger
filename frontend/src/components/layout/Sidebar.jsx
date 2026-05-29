import { useEffect, useState } from 'react'
import { NavLink } from 'react-router-dom'
import { useRepos } from '../../api/runs'
import ThemeToggle from './ThemeToggle.jsx'

function dotColor(conclusion) {
  if (conclusion === 'SUCCESS') return 'var(--color-pass-bar)'
  if (conclusion === 'FAILURE') return 'var(--color-fail-bar)'
  if (conclusion === 'IN_PROGRESS') return 'var(--color-progress-bar)'
  return 'var(--color-text-muted)'
}

const navLinkStyle = ({ isActive }) => ({
  display: 'flex',
  alignItems: 'center',
  padding: '7px 1rem',
  fontSize: '14px',
  textDecoration: 'none',
  color: isActive ? 'var(--color-accent-text)' : 'var(--color-text-secondary)',
  fontWeight: isActive ? '500' : '400',
  background: isActive ? 'var(--color-accent-bg)' : 'transparent',
  borderRight: isActive ? '2px solid var(--color-accent)' : '2px solid transparent',
})

export default function Sidebar() {
  const [theme, setTheme] = useState(() => localStorage.getItem('theme') || 'light')

  const { data: repos, isLoading } = useRepos()

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
    localStorage.setItem('theme', theme)
  }, [theme])

  const reposByOwner =
    repos?.reduce((acc, repo) => {
      if (!acc[repo.owner]) acc[repo.owner] = []
      acc[repo.owner].push(repo)
      return acc
    }, {}) ?? {}

  return (
    <aside
      className="flex flex-col flex-shrink-0"
      style={{
        width: '220px',
        background: 'var(--color-bg-sidebar)',
        borderRight: '0.5px solid var(--color-border)',
      }}
    >
      <div
        style={{
          padding: '0 1rem',
          borderBottom: '0.5px solid var(--color-border)',
          height: '72px',
          display: 'flex',
          alignItems: 'center',
        }}
      >
        <span style={{ fontSize: '15px', fontWeight: '500', color: 'var(--color-accent)' }}>
          CI Debugger
        </span>
      </div>

      <nav className="flex flex-col flex-1" style={{ padding: '8px 0', overflowY: 'auto' }}>
        <NavLink to="/" end style={navLinkStyle}>
          Dashboard
        </NavLink>

        {!isLoading &&
          Object.entries(reposByOwner).map(([owner, ownerRepos]) => (
            <div key={owner}>
              <p
                style={{
                  padding: '1rem 1rem 4px',
                  fontSize: '11px',
                  color: 'var(--color-text-muted)',
                  textTransform: 'uppercase',
                  letterSpacing: '0.06em',
                  margin: 0,
                }}
              >
                {owner}
              </p>
              {ownerRepos.map((repo) => (
                <NavLink
                  key={repo.repo}
                  to={`/${repo.owner}/${repo.repo}`}
                  style={({ isActive }) => ({
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    padding: '5px 1rem 5px 1.25rem',
                    fontSize: '13px',
                    textDecoration: 'none',
                    color: isActive ? 'var(--color-accent-text)' : 'var(--color-text-secondary)',
                    background: isActive ? 'var(--color-accent-bg)' : 'transparent',
                    borderRight: isActive
                      ? '2px solid var(--color-accent)'
                      : '2px solid transparent',
                  })}
                >
                  <span
                    style={{
                      width: '6px',
                      height: '6px',
                      borderRadius: '50%',
                      flexShrink: 0,
                      background: dotColor(repo.overallConclusion),
                    }}
                  />
                  {repo.repo}
                </NavLink>
              ))}
            </div>
          ))}
      </nav>

      <div style={{ padding: '1rem', borderTop: '0.5px solid var(--color-border)' }}>
        <ThemeToggle theme={theme} onToggle={setTheme} />
      </div>
    </aside>
  )
}
