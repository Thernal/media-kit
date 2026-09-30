# Agent skills

Skills for AI coding agents working in projects that **use** media-kit, in the
[Agent Skills](https://agentskills.io) layout.

| Skill | Use it when |
|---|---|
| [`media-kit`](media-kit/SKILL.md) | picking images (gallery, several, camera, documents) or files, size and type limits, the camera permission, cropping an image, a zoomable image viewer, testing picker-driven UI |

```
media-kit/
├── SKILL.md               the model, orientation, rules, task router
└── references/
    ├── setup.md           modules, wiring, the root, manifest and Info.plist, replacing the camera permission
    ├── picking.md         images, several, camera, files, results and rejections, previews and tests
    └── cropping.md        the crop screen, custom bars, options, the cropper alone, the zoomable preview
```
