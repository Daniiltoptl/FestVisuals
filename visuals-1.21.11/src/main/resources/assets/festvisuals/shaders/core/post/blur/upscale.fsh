#version 330

#moj_import <festvisuals:common.glsl>

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 halfTexel = uShape.xy;
    float offset = uShape.z;

    fragColor = (
        texture(InSampler, texCoord + vec2(-halfTexel.x * 2, 0) * offset) +
        texture(InSampler, texCoord + vec2(-halfTexel.x, halfTexel.y) * offset) * 2 +
        texture(InSampler, texCoord + vec2(0, halfTexel.y * 2) * offset) +
        texture(InSampler, texCoord + halfTexel * offset) * 2 +
        texture(InSampler, texCoord + vec2(halfTexel.x * 2, 0) * offset) +
        texture(InSampler, texCoord + vec2(halfTexel.x, -halfTexel.y) * offset) * 2 +
        texture(InSampler, texCoord + vec2(0, -halfTexel.y * 2) * offset) +
        texture(InSampler, texCoord - halfTexel * offset) * 2
    ) / 12;

    fragColor.a = 1;
}
