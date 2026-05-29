import { useQuery } from '@tanstack/react-query'

async function fetchOpenPullRequests() {
  const res = await fetch('/api/pull-requests/open')
  if (!res.ok) throw new Error(`Failed to fetch open pull requests: ${res.status}`)
  return res.json()
}

async function fetchPullRequest(id, page) {
  const res = await fetch(`/api/pull-requests/${id}?page=${page}`)
  if (!res.ok) throw new Error(`Failed to fetch pull request: ${res.status}`)
  return res.json()
}

async function fetchRepoPullRequests(owner, repo, status, page) {
  const res = await fetch(`/api/pull-requests/${owner}/${repo}?status=${status}&page=${page}`)
  if (!res.ok) throw new Error(`Failed to fetch repo pull requests: ${res.status}`)
  return res.json()
}

async function fetchRunSets(id, page) {
  const res = await fetch(`/api/pull-requests/${id}/run-sets?page=${page}`)
  if (!res.ok) throw new Error(`Failed to fetch run sets: ${res.status}`)
  return res.json()
}

export function useOpenPullRequests() {
  return useQuery({
    queryKey: ['pull-requests', 'open'],
    queryFn: fetchOpenPullRequests,
    refetchInterval: 30000,
  })
}

export function usePullRequest(id, page) {
  return useQuery({
    queryKey: ['pull-requests', id, page],
    queryFn: () => fetchPullRequest(id, page),
    refetchInterval: 30000,
  })
}

export function useRepoPullRequests(owner, repo, status, page) {
  return useQuery({
    queryKey: ['pull-requests', owner, repo, status, page],
    queryFn: () => fetchRepoPullRequests(owner, repo, status, page),
  })
}

export function useRunSets(id, page) {
  return useQuery({
    queryKey: ['pull-requests', id, 'run-sets', page],
    queryFn: () => fetchRunSets(id, page),
    refetchInterval: 30000,
  })
}
