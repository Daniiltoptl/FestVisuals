#version 330

#moj_import <festvisuals:common.glsl>

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 halfTexel = uShape.xy;
    float offset = uShape.z;

    fragColor = (
        texture(InSampler, texCoord) * 4 +
        texture(InSampler, texCoord - halfTexel * offset) +
        texture(InSampler, texCoord + halfTexel * offset) +
        texture(InSampler, texCoord + vec2(halfTexel.x, -halfTexel.y) * offset) +
        texture(InSampler, texCoord - vec2(halfTexel.x, -halfTexel.y) * offset)
    ) / 8;

    fragColor.a = 1;
}
