#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <festvisuals:common.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;

out vec2 FragCoord;
out vec2 TexCoord;

void main() {
    FragCoord = rvertexcoord(gl_VertexID);
    TexCoord = UV0;

    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
