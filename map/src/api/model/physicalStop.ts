type PhysicalStop = {
    relationalId: number
    externalId: string
    name: string
    position: number[]
    tags: Record<string, string>
}

export type { PhysicalStop }
