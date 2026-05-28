import { useQuery } from '@tanstack/react-query'

async function fetchRuns() {
  const res = await fetch('/api/runs')
  if (!res.ok) throw new Error(`Failed to fetch runs: ${res.status}`)
  return res.json()
}

async function fetchRepoRuns(owner, repo, page) {
  const res = await fetch(`/api/runs/${owner}/${repo}?page=${page}`)
  if (!res.ok) throw new Error(`Failed to fetch repo runs: ${res.status}`)
  return res.json()
}

async function fetchRun(id) {
  const res = await fetch(`/api/runs/${id}`)
  if (!res.ok) throw new Error(`Failed to fetch run: ${res.status}`)
  return res.json()
}

async function fetchRunSteps(id) {
  const res = await fetch(`/api/runs/${id}/steps`)
  if (!res.ok) throw new Error(`Failed to fetch run steps: ${res.status}`)
  return res.json()
}

async function fetchRunClusters(id) {
  const res = await fetch(`/api/runs/${id}/clusters`)
  if (!res.ok) throw new Error(`Failed to fetch run clusters: ${res.status}`)
  return res.json()
}

export function useRuns() {
  return useQuery({
    queryKey: ['runs'],
    queryFn: fetchRuns,
  })
}

export function useRepoRuns(owner, repo, page) {
  return useQuery({
    queryKey: ['runs', owner, repo, page],
    queryFn: () => fetchRepoRuns(owner, repo, page),
  })
}

export function useRun(id) {
  return useQuery({
    queryKey: ['runs', id],
    queryFn: () => fetchRun(id),
  })
}

export function useRunSteps(id) {
  return useQuery({
    queryKey: ['runs', id, 'steps'],
    queryFn: () => fetchRunSteps(id),
  })
}

export function useRunClusters(id) {
  return useQuery({
    queryKey: ['runs', id, 'clusters'],
    queryFn: () => fetchRunClusters(id),
  })
}
