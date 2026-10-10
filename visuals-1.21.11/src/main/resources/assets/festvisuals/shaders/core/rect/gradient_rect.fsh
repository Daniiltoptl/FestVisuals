#version 330

#moj_import <festvisuals:common.glsl>

in vec2 FragCoord;

out vec4 fragColor;

vec4 blend(vec2 uv) {
    vec4 top = mix(uTopLeftColor, uTopRightColor, uv.x);
    vec4 bottom = mix(uBottomLeftColor, uBottomRightColor, uv.x);

    return mix(top, bottom, uv.y);
}

void main() {
    vec4 gradient = blend(FragCoord);

    float alpha = ralpha(uSize, FragCoord, uRadius, uSmoothness);
    vec4 color = vec4(gradient.rgb, gradient.a * alpha);

    if (color.a == 0.0) {
        discard;
    }

    fragColor = color;
}
