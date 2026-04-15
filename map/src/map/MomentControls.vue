<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { d } = useI18n()

const moment = defineModel<Date>({ required: true })
const playing = defineModel<boolean>('playing', { required: true })
const playSpeed = defineModel<number>('play-speed', { required: true })
const playSpeedOptions = [180, 60, 10, 1].map((s) => ({ value: s, title: `${s}x` }))

function onDateUpdate(date: Date | undefined) {
    if (date == undefined) return
    const newMoment = new Date(moment.value)
    newMoment.setFullYear(date.getFullYear(), date.getMonth(), date.getDate())
    moment.value = newMoment
}
function onHourUpdate(hour: number | undefined) {
    if (hour == undefined) return
    const newMoment = new Date(moment.value)
    newMoment.setHours(hour)
    moment.value = newMoment
}
function onMinuteUpdate(minute: number | undefined) {
    if (minute == undefined) return
    const newMoment = new Date(moment.value)
    newMoment.setMinutes(minute)
    moment.value = newMoment
}
function onSecondUpdate(second: number | undefined) {
    if (second == undefined) return
    const newMoment = new Date(moment.value)
    newMoment.setSeconds(second)
    moment.value = newMoment
}
const notEmptyValidationRule = (v: unknown) => !!v || v === 0
</script>

<template>
    <div class="moment-controls">
        <v-card class="row-flex form-card">
            <v-icon icon="mdi-clock-outline" />
            <span v-show="playing">
                {{ d(moment, 'long') }}
            </span>
            <v-form v-show="!playing" class="moment-form row-flex">
                <v-date-input
                    :model-value="moment"
                    prepend-icon=""
                    variant="outlined"
                    density="comfortable"
                    hide-details
                    :disabled="playing"
                    :picker-props="{ color: 'primary' }"
                    @update:model-value="onDateUpdate"
                />
                -
                <v-number-input
                    :model-value="moment.getHours()"
                    :rules="[notEmptyValidationRule]"
                    variant="outlined"
                    control-variant="hidden"
                    hide-details
                    :disabled="playing"
                    density="comfortable"
                    @update:model-value="onHourUpdate"
                />
                :
                <v-number-input
                    :model-value="moment.getMinutes()"
                    :rules="[notEmptyValidationRule]"
                    variant="outlined"
                    control-variant="hidden"
                    hide-details
                    :disabled="playing"
                    density="comfortable"
                    @update:model-value="onMinuteUpdate"
                />
                :
                <v-number-input
                    :model-value="moment.getSeconds()"
                    :rules="[notEmptyValidationRule]"
                    variant="outlined"
                    control-variant="hidden"
                    hide-details
                    :disabled="playing"
                    density="comfortable"
                    @update:model-value="onSecondUpdate"
                />
            </v-form>
        </v-card>
        <v-card class="row-flex">
            <div class="speed-input-wrapper row-flex">
                <v-icon icon="mdi-speedometer" />
                <v-select
                    v-model="playSpeed"
                    :items="playSpeedOptions"
                    variant="outlined"
                    hide-details
                    density="comfortable"
                />
            </div>
        </v-card>
        <v-btn
            :icon="playing ? 'mdi-pause' : 'mdi-play'"
            color="primary"
            @click="playing = !playing"
        />
    </div>
</template>

<style scoped>
.moment-controls {
    display: flex;
    flex-direction: row;
    gap: 0.5em;
    align-items: center;
}
.row-flex {
    display: flex;
    flex-direction: row;
    align-items: center;
}
.v-card {
    padding: 0.125em 0.5em;
    gap: 0.25em;
    height: 3.25em;
}
.moment-form {
    gap: 0.25em;
}
.moment-controls .v-date-input {
    width: 6em;
}
.moment-controls .v-number-input {
    width: 2em;
}
.moment-controls .v-select {
    width: 5em;
}
.moment-controls .v-select :deep(.v-field__input) {
    justify-content: center;
    padding-inline-end: 0 !important;
}
.moment-controls .v-select :deep(.v-field) {
    padding-inline-end: 0.125em;
}
.moment-controls :deep(.v-field__input) {
    text-align: center;
    padding: 0 0.25em !important;
}
.moment-controls :deep(.v-field__input) input {
    text-align: center;
}
.speed-input-wrapper {
    gap: 0.5em;
}
</style>
