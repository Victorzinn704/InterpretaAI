# InterpretaAI interface system

## Intent
- Child experience: one clear decision per viewport, playful comic rhythm, large targets.
- Teacher experience: a calm teaching notebook that follows the same story from preparation through classroom evidence.
- Shared signature: a visible story journey connecting review, publication, device preparation, activity and observation.

## Color and type
- Foundation: paper cream #FFFDF5, ink #111C2D, white #FFFEFA.
- Action and state: yellow #FACC15, blue #0284C7, green #16A34A, red #E11D48.
- Adult surfaces use color for current state and primary action. Child surfaces may use the full comic palette.
- Use the platform sans font with strong, short headings. Avoid long all-caps paragraphs.

## Depth and shape
- Child panels: 3 dp ink border, 4 to 6 dp hard shadow, 18 to 24 dp radius.
- Teacher panels: 1 to 2 dp neutral or ink border; hard shadow only on the current story and primary action.
- No gradients. Use cream paper, flat color and borders.

## Spacing and responsiveness
- Base spacing unit: 4 dp. Common gaps: 8, 12, 16, 24 and 32 dp.
- Phone: single column with 16 dp horizontal padding.
- Tablet from 720 dp: 28 dp horizontal padding and larger section headings.
- Child screens still require one action without scrolling at 360x640, 412x915 and 800x1280.
- Teacher screens may scroll, but advanced settings stay collapsed until requested.

## Components
- ComicPanel: story focus, preview or a bounded teaching decision.
- ComicButton: one primary action per section; white buttons are secondary.
- Pill: compact identity or state, never a paragraph.
- lesson-journey: four lifecycle steps using exact server and device states.
- Adult navigation labels: História, Turma, Tablet. Keep infrastructure details behind Tablet.
