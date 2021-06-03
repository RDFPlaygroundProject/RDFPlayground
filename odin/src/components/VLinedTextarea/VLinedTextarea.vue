<template>
  <div class="v-lined-textarea">
    <div
      class="lined-textarea__lines"
      v-if="!disabled"
      :style="{ 'padding-right': longestWidth + 'px' }"
    >
      <div class="lined-textarea__lines__inner" ref="lines">
        <p
          v-for="(line, index) in lines"
          :key="index"
          class="lined-textarea__lines__line"
          :class="{
            'lined-textarea__lines__line--invalid': invalidLines.includes(line),
          }"
          v-html="line"
        ></p>
      </div>
    </div>

    <v-textarea
      :disabled="disabled_input"
      :placeholder="placeholder"
      class="lined-textarea__content monospaced-area pt-3"
      :class="{
        'v-lined-textarea__content--disabled': disabled,
        'v-lined-textarea__content--wrap': !nowrap,
        'v-lined-textarea__content--nowrap': nowrap,
      }"
      v-model="content"
      v-on:scroll="scrollLines"
      v-on:input="onInput"
      v-on:mousedown="detectResize"
      :dark="dark"
      :counter="counter"
      :outlined="outlined"
      :readonly="readonly"
      :no-resize="no_resize"
      :auto-grow="autogrow"
      :rows="rows"
      :label="label"
      :hint="hint"
      :style="styles"
      ref="aTextarea"
    ></v-textarea>
    <div class="count-helper" ref="helper"></div>
  </div>
</template>

<script src="./VLinedTextarea.js">
</script>

<style scoped>
@import "./VLinedTextarea.css";
</style>
