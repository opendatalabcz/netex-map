export type PopUpMessageType = 'ERROR' | 'INFO'

export type PopUpMessage = {
    messageKey: string
    messageArguments?: Record<string, string> | undefined
    type: PopUpMessageType
}

export type MessageDisplay = {
    enqueue(message: PopUpMessage): void
}

export class PopUpMessageController {
    private unhandledMessagesQueue: PopUpMessage[] = []
    private messageDisplay: MessageDisplay | null = null

    enqueue(message: PopUpMessage) {
        if (this.messageDisplay == null) {
            this.unhandledMessagesQueue.push(message)
            return
        }
        this.messageDisplay.enqueue(message)
    }

    setMessageDisplay(display: MessageDisplay | null) {
        this.messageDisplay = display
        if (display != null) {
            for (const unhandledMessage of this.unhandledMessagesQueue) {
                display.enqueue(unhandledMessage)
            }
            this.unhandledMessagesQueue = []
        }
    }
}

const ThePopUpMessageController = new PopUpMessageController()
export default ThePopUpMessageController
