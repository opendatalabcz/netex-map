class LocalTime {
    private hours
    private minutes
    private seconds

    constructor(hours = 0, minutes = 0, seconds = 0) {
        this.hours = Math.max(0, Math.min(23, hours))
        this.minutes = Math.max(0, Math.min(59, minutes))
        this.seconds = Math.max(0, Math.min(59, seconds))
    }

    static parse(timeString: string) {
        const [h, m, s] = timeString.split(':').map(Number)
        return new LocalTime(h, m ?? 0, s ?? 0)
    }

    toString() {
        const pad = (n: number) => String(n).padStart(2, '0')
        return `${this.hours}:${pad(this.minutes)}:${pad(this.seconds)}`
    }

    get hour() { return this.hours }
    get minute() { return this.minutes }
    get second() { return this.seconds }

    isAfter(other: LocalTime) {
        return this.toTotalSeconds() > other.toTotalSeconds()
    }

    toTotalSeconds() {
        return this.hours * 3600 + this.minutes * 60 + this.seconds
    }
}

export default LocalTime
