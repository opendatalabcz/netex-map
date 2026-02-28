const MILlIS_IN_SECOND = 1000
const MILLIS_IN_MINUTE = 60 * MILlIS_IN_SECOND
const MILLIS_IN_HOUR = 60 * MILLIS_IN_MINUTE
const MILLIS_IN_DAY = 24 * MILLIS_IN_HOUR

class LocalTime {
    readonly totalMillis: number

    static FIRST_MOMENT_IN_DAY = new LocalTime(0)
    static LAST_MOMENT_IN_DAY = new LocalTime(MILLIS_IN_DAY - 1)

    constructor(totalMillis: number) {
        this.totalMillis = totalMillis % MILLIS_IN_DAY
    }

    static of(hours = 0, minutes = 0, seconds = 0, millis = 0) {
        return new LocalTime(hours * MILLIS_IN_HOUR + minutes * MILLIS_IN_MINUTE + seconds * MILlIS_IN_SECOND + millis)
    }

    private static parseIntOrZero(string: string): number {
        const parsed = Number.parseInt(string)
        return Number.isNaN(parsed) ? 0 : parsed
    }

    static parse(timeString: string) {
        const colonSplit = timeString.split(':')
        const dotSplit = colonSplit[2]?.split('.')
        const hours = colonSplit[0] != null ? LocalTime.parseIntOrZero(colonSplit[0]) : 0
        const minutes = colonSplit[1] != null ? LocalTime.parseIntOrZero(colonSplit[1]) : 0
        const seconds = dotSplit?.[0] != null ? LocalTime.parseIntOrZero(dotSplit[0]) : 0
        const millis = dotSplit?.[1] != null ? LocalTime.parseIntOrZero(dotSplit[1].substring(0, 3)) : 0
        return LocalTime.of(hours, minutes, seconds, millis)
    }

    static ofDate(date: Date) {
        return LocalTime.of(date.getHours(), date.getMinutes(), date.getSeconds())
    }

    get hour() { return Math.floor(this.totalMillis / MILLIS_IN_HOUR) }
    get minute() { return Math.floor((this.totalMillis % MILLIS_IN_HOUR) / MILLIS_IN_MINUTE) }
    get second() { return Math.floor((this.totalMillis % MILLIS_IN_MINUTE) / MILlIS_IN_SECOND) }
    get millis() { return this.totalMillis % MILlIS_IN_SECOND }

    isBefore(other: LocalTime) {
        return this.totalMillis < other.totalMillis
    }

    isAfter(other: LocalTime) {
        return this.totalMillis > other.totalMillis
    }

    toString() {
        const baseTime = `${this.hour}:${this.minute.toString().padStart(2)}:${this.second.toString().padStart(2)}`
        const millis = this.millis
        return millis === 0 ? baseTime : baseTime + millis.toString().padStart(3)
    }
}

export default LocalTime
