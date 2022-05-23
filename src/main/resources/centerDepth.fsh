#version 150 core

uniform sampler2D depth;
uniform sampler2D altDepth;
uniform float lastFrameTime;
uniform float decay;

out vec4 iris_fragColor;

void main() {
    float currentDepth = texture(depth, vec2(0.5)).r;
    float decay2 = 1.0 - exp(-decay * lastFrameTime);
    iris_fragColor = vec4(mix(texture(altDepth, vec2(0.5)).r, currentDepth, decay2), 0, 0, 0);
}
