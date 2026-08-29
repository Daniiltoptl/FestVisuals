#version 330

layout(std140) uniform EvaParams {
    vec4 uRadius;
    vec4 uShape;
    vec4 uTopLeftColor;
    vec4 uBottomLeftColor;
    vec4 uBottomRightColor;
    vec4 uTopRightColor;
    vec4 uText;
    vec4 uOutlineColor;
};

#define uSize (uShape.xy)
#define uSmoothness (uShape.z)
#define uMix (uShape.w)
#define uAlpha (uText.x)
#define uRange (uText.x)
#define uThickness (uText.y)
#define uOutline (uText.z)
#define uOutlineThickness (uText.w)

float rdist(vec2 pos, vec2 size, vec4 radius) {
    radius.xy = (pos.x > 0.0) ? radius.xy : radius.wz;
    radius.x  = (pos.y > 0.0) ? radius.x : radius.y;

    vec2 v = abs(pos) - size + radius.x;
    return min(max(v.x, v.y), 0.0) + length(max(v, 0.0)) - radius.x;
}

float ralpha(vec2 size, vec2 coord, vec4 radius, float smoothness) {
    vec2 center = size * 0.5;
    float dist = rdist(center - (coord * size), center - 1.0, radius);
    return 1.0 - smoothstep(1.0 - smoothness, 1.0, dist);
}

const vec2[4] RECT_VERTICES_COORDS = vec2[] (
    vec2(0.0, 0.0),
    vec2(0.0, 1.0),
    vec2(1.0, 1.0),
    vec2(1.0, 0.0)
);

vec2 rvertexcoord(int id) {
    return RECT_VERTICES_COORDS[id % 4];
}
