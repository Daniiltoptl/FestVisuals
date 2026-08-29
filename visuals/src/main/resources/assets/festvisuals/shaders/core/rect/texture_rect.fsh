#version 330

#moj_import <festvisuals:common.glsl>

uniform sampler2D Sampler0;

in vec2 FragCoord;
in vec2 TexCoord;
in vec4 FragColor;

out vec4 fragColor;

void main() {
    float alpha = ralpha(uSize, FragCoord, uRadius, uSmoothness);
    vec4 color = vec4(1.0, 1.0, 1.0, alpha) * texture(Sampler0, TexCoord) * FragColor;

    if (color.a == 0.0) {
        discard;
    }

    fragColor = color;
}
