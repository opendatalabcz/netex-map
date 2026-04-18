<script setup lang="ts">
import type {
    MessageDisplay,
    PopUpMessage,
    PopUpMessageController,
} from '@/services/popUpMessageController'
import type { SnackbarMessage } from 'vuetify/lib/components/VSnackbarQueue/VSnackbarQueue.mjs'
import { onMounted, onUnmounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const props = defineProps<{
    controller: PopUpMessageController
}>()

const ERROR_DISPLAY_TIME_MILLIS = 5000
const INFO_DISPLAY_TIME_MILLIS = 3000
const messages = ref<SnackbarMessage[]>([])
const display: MessageDisplay = {
    enqueue(message: PopUpMessage) {
        messages.value.push({
            text: t(message.messageKey, message.messageArguments ?? {}),
            color: message.type === 'ERROR' ? 'error' : 'info',
            timeout:
                message.type === 'ERROR' ? ERROR_DISPLAY_TIME_MILLIS : INFO_DISPLAY_TIME_MILLIS,
        })
    },
}
onMounted(() => props.controller.setMessageDisplay(display))
onUnmounted(() => props.controller.setMessageDisplay(null))
</script>

<template>
    <v-snackbar-queue v-model="messages" class="snack-bar" closable :total-visible="3">
        <template #actions="actionsProps">
            <v-btn v-bind="actionsProps.props" icon variant="text" size="x-small">
                <v-icon icon="mdi-close" size="x-small" />
            </v-btn>
        </template>
    </v-snackbar-queue>
</template>
