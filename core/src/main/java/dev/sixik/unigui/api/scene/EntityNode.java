package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.docs.WikiThrows;
import dev.sixik.unigui.api.math.Aabb;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * A Minecraft entity model with its feet at the local origin.
 *
 * <pre>{@code
 * new EntityNode("minecraft:zombie");
 * EntityNode.of(entity);
 * EntityNode.of(EntityType.ZOMBIE);
 * }</pre>
 * <p>A preview by type requires a client world and is recreated when the world changes. While no world is
 * loaded the node retries later. For typed access use a helper such as {@code MinecraftScene.entity(node)}.
 * Unsupported platforms skip rendering and log a warning once.</p>
 */
@WikiPage(
        title = "EntityNode", slug = "entity-node",
        category = "API/3D Scene", order = 150,
        summary = "A Minecraft entity preview with body transform and optional head rotation.")
@WikiSection(title = "Sources and pose", order = 10, markdown = """
        Create a type preview using a registry ID, or pass a native entity or type to `EntityNode.of(...)`. A type
        preview is temporarily absent until its world is available. `clearHead()` removes the explicit head
        override.
        """)
@WikiSection(title = "<key:entity.node.section.animation.title>", order = 20, markdown = "<key:entity.node.section.animation.body>")
@WikiExample(title = "<key:entity.node.example.walk.title>", code = """
        var zombie = new EntityNode("minecraft:zombie");
        scene.addRoot(zombie);
        timeline.track(zombie)
                .position(0, 0, 0, 0).position(2, 0, 0, 2)
                .walk(0, 0, 0.6f).walk(2, 14, 0.6f).walk(2.2f, 14, 0);
        """)
@WikiExample(title = "<key:entity.node.example.golem.title>", code = """
        var golem = new EntityNode("minecraft:iron_golem");
        scene.addRoot(golem);
        golem.setNativeAnimation(IronGolemRenderState.class, state -> {
            float attackTick = state.ageInTicks - 20;
            state.attackTicksRemaining = attackTick >= 0 && attackTick < 10 ? 10 - attackTick : 0;
        });
        timeline.onSample(seconds -> golem.setAnimation(seconds, 0, 0));
        """)
@WikiExample(title = "Preview a zombie looking down", code = """
        EntityNode zombie = new EntityNode("minecraft:zombie");
        zombie.setPosition(2, 0, 0).setHead(0.35f, 0.2f);
        viewport.highlightObjects(true).scene().addRoot(zombie);
        """)
@WikiLink(label = "SceneNode", target = "page:scene-node")
@WikiLink(label = "ScenePlatform", target = "page:scene-platform")
@WikiLink(label = "Scene3D", target = "page:scene-3d")
@WikiLink(label = "SceneTimeline", target = "page:scene-timeline")
@WikiLink(label = "EntityActionClip", target = "page:entity-action-clip")
@AiDoc(
        summary = "A Minecraft entity preview with body transform and optional head rotation.",
        purpose = "Display an entity or an entity type in a UI scene.",
        criticalRules = {
                "The local origin is at the feet and the unrotated body faces +Z.",
                "A type preview may need a loaded client world and can be recreated on world change.",
                "`setHead` uses radians. Positive yaw turns toward the entity's right shoulder. Positive pitch tilts down. Use the node's rotation for the body.",
                "GTNH 1.7.10, Cleanroom 1.12.2 and Minecraft 1.20.1, 1.21.1 and 26.x apply setAnimation and timeline walk keys to native idle and locomotion. Move the SceneNode to control travel.",
                "On Minecraft 26.x, use setNativeAnimation for mob-specific render-state fields. Write a complete pose on every call and use the sampled ageInTicks as the clock."
        },
        related = {"SceneNode", "ScenePlatform", "Scene3D", "SceneTimeline"},
        examples = @AiDoc.Example(title = "Preview a zombie looking down", code = """
                EntityNode zombie = new EntityNode("minecraft:zombie");
                zombie.setPosition(2, 0, 0).setHead(0.35f, 0.2f);
                viewport.highlightObjects(true).scene().addRoot(zombie);
                """))
public final class EntityNode extends SceneNode {
    /**
     * Entity type ID, or null for native input.
     */
    private final String typeId;
    /**
     * Native source object, or null for a type ID.
     */
    private final Object nativeSource;
    /**
     * Cache of the resolved entity.
     */
    private final ResolvedContent entity = new ResolvedContent();
    /**
     * Whether the head orientation overrides the entity's own pose.
     */
    private boolean head;
    /**
     * Head yaw and pitch relative to the body, in radians.
     */
    private float headYaw, headPitch;
    /**
     * Whether the scene supplies the native model clock and locomotion.
     */
    private boolean animation;
    /**
     * Absolute playback time in seconds, accumulated gait phase and normalized stride strength.
     */
    private float animationSeconds, walkPhase, walkStrength;
    /**
     * Optional callback for the extracted render state of a particular backend type.
     */
    private Consumer<Object> nativeAnimation;
    /**
     * Action selected by the last sample of a timeline with action clips for this node.
     */
    private EntityActionClip<?> action;
    /**
     * Normalized phase of the selected clip's current cycle, in [0, 1).
     */
    private float actionProgress;

    /**
     * Creates an entity preview from a type ID.
     *
     * @param typeId entity type ID such as {@code minecraft:zombie}.
     * @throws IllegalArgumentException if typeId is blank.
     */
    @WikiMethod(section = "Sources", sectionOrder = 10, order = 10,
            summary = "Creates an entity preview from a type ID.")
    @WikiThrows(type = IllegalArgumentException.class, description = "if typeId is blank.")
    @AiDoc(section = "Sources", sectionOrder = 10, order = 10,
            summary = "Creates an entity preview from a type ID.")
    public EntityNode(
            @WikiParameter(description = "entity type ID such as `minecraft:zombie`.") String typeId) {
        String trimmed = Objects.requireNonNull(typeId, "typeId").trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException("Entity type id is empty");
        this.typeId = trimmed;
        this.nativeSource = null;
    }

    /**
     * Creates a node from native input.
     *
     * @param source native entity or type object.
     */
    private EntityNode(Object source) {
        this.typeId = null;
        this.nativeSource = Objects.requireNonNull(source, "entity");
    }

    /**
     * Creates a node from an entity, native type or ID.
     *
     * @param entity native entity, native type or ID string.
     * @return a new node.
     * @throws NullPointerException if entity is null.
     */
    @WikiMethod(section = "Sources", sectionOrder = 10, order = 20,
            summary = "Creates a node from an entity, native type or ID.")
    @WikiReturn("a new node")
    @WikiThrows(type = NullPointerException.class, description = "if entity is null.")
    @AiDoc(section = "Sources", sectionOrder = 10, order = 20,
            summary = "Creates a node from an entity, native type or ID.")
    public static EntityNode of(
            @WikiParameter(description = "native entity, native type or ID string.") Object entity) {
        if (entity instanceof String text) return new EntityNode(text);
        return new EntityNode(entity);
    }

    /**
     * Rotates the head relative to the body. Zero yaw faces local +Z, positive yaw turns toward
     * the entity's right shoulder, and positive pitch tilts down. Use {@link #setRotation} for the body.
     *
     * @param yaw   yaw in radians.
     * @param pitch pitch in radians.
     * @return this node.
     * @throws IllegalArgumentException if either angle is nonfinite.
     */
    @WikiMethod(section = "Pose", sectionOrder = 20, order = 10,
            summary = "Rotates the head relative to the body. Zero yaw faces local +Z, positive yaw turns toward the entity's right shoulder, and positive pitch tilts down. Use `setRotation` for the body.")
    @WikiReturn("this node")
    @WikiThrows(type = IllegalArgumentException.class, description = "if either angle is nonfinite.")
    @AiDoc(section = "Pose", sectionOrder = 20, order = 10,
            summary = "Rotates the head relative to the body. Zero yaw faces local +Z, positive yaw turns toward the entity's right shoulder, and positive pitch tilts down. Use `setRotation` for the body.")
    public EntityNode setHead(
            @WikiParameter(description = "yaw in radians.") float yaw,
            @WikiParameter(description = "pitch in radians.") float pitch) {
        if (!Float.isFinite(yaw) || !Float.isFinite(pitch))
            throw new IllegalArgumentException("Head angles must be finite");
        if (!head || headYaw != yaw || headPitch != pitch) {
            head = true;
            headYaw = yaw;
            headPitch = pitch;
            invalidateContent();
        }
        return this;
    }

    /**
     * Restores the entity's own head pose.
     *
     * @return this node.
     */
    @WikiMethod(section = "Pose", sectionOrder = 20, order = 20,
            summary = "Restores the entity's own head pose.")
    @WikiReturn("this node")
    @AiDoc(section = "Pose", sectionOrder = 20, order = 20,
            summary = "Restores the entity's own head pose.")
    public EntityNode clearHead() {
        if (head) {
            head = false;
            invalidateContent();
        }
        return this;
    }

    /**
     * Checks whether the node overrides the head pose.
     *
     * @return true after {@link #setHead}, until {@link #clearHead}.
     */
    @WikiMethod(section = "Pose", sectionOrder = 20, order = 30,
            summary = "Checks whether the node overrides the head pose.")
    @WikiReturn("true after `setHead`, until `clearHead`")
    @AiDoc(section = "Pose", sectionOrder = 20, order = 30,
            summary = "Checks whether the node overrides the head pose.")
    public boolean hasHead() {
        return head;
    }

    /**
     * Reads the head yaw.
     *
     * @return yaw relative to the body in radians.
     */
    @WikiMethod(section = "Pose", sectionOrder = 20, order = 40,
            summary = "Reads the head yaw.")
    @WikiReturn("yaw relative to the body in radians")
    @AiDoc(section = "Pose", sectionOrder = 20, order = 40,
            summary = "Reads the head yaw.")
    public float headYaw() {
        return headYaw;
    }

    /**
     * Reads the head pitch.
     *
     * @return pitch in radians, positive downward.
     */
    @WikiMethod(section = "Pose", sectionOrder = 20, order = 50,
            summary = "Reads the head pitch.")
    @WikiReturn("pitch in radians, positive downward")
    @AiDoc(section = "Pose", sectionOrder = 20, order = 50,
            summary = "Reads the head pitch.")
    public float headPitch() {
        return headPitch;
    }

    /**
     * Overrides the native model clock and locomotion on GTNH 1.7.10, Cleanroom 1.12.2 and Minecraft 1.20.1, 1.21.1 and 26.x.
     *
     * @param seconds      absolute animation time in seconds, finite and nonnegative
     * @param walkPhase    accumulated gait phase in vanilla model units
     * @param walkStrength stride strength in [0, 1]
     * @return this node
     * @throws IllegalArgumentException if any input is nonfinite or outside its range
     */
    @WikiMethod(section = "<key:scene.api.section.animation>", sectionOrder = 30, summary = "<key:entity.node.api.set.animation.summary>")
    @WikiReturn("<key:scene.api.return.node>")
    @WikiThrows(type = IllegalArgumentException.class, description = "<key:entity.node.api.set.animation.throws.IllegalArgumentException>")
    @AiDoc(section = "Animation", summary = "Samples idle time in seconds, accumulated gait phase and normalized stride strength.")
    public EntityNode setAnimation(
            @WikiParameter(description = "<key:entity.node.api.set.animation.param.seconds>") float seconds,
            @WikiParameter(description = "<key:entity.node.api.set.animation.param.phase>") float walkPhase,
            @WikiParameter(description = "<key:entity.node.api.set.animation.param.strength>") float walkStrength) {
        if (!Float.isFinite(seconds) || seconds < 0 || !Float.isFinite(walkPhase)
                || !(walkStrength >= 0 && walkStrength <= 1)) {
            throw new IllegalArgumentException("Invalid entity animation time or locomotion");
        }
        if (!animation || animationSeconds != seconds || this.walkPhase != walkPhase || this.walkStrength != walkStrength) {
            animation = true;
            animationSeconds = seconds;
            this.walkPhase = walkPhase;
            this.walkStrength = walkStrength;
            invalidateContent();
        }
        return this;
    }

    /**
     * Restores the native entity's clock and locomotion.
     *
     * @return this node
     */
    @WikiMethod(section = "<key:scene.api.section.animation>", sectionOrder = 30, summary = "<key:entity.node.api.clear.animation.summary>")
    @WikiReturn("<key:scene.api.return.node>")
    @AiDoc(section = "Animation", summary = "Removes the explicit clock and locomotion override.")
    public EntityNode clearAnimation() {
        if (animation) {
            animation = false;
            invalidateContent();
        }
        return this;
    }

    /**
     * @return whether the model clock and locomotion are overridden
     */
    public boolean hasAnimation() {
        return animation;
    }

    /**
     * @return the last explicit animation time in seconds
     */
    public float animationSeconds() {
        return animationSeconds;
    }

    /**
     * @return the last explicit gait phase in native model units
     */
    public float walkPhase() {
        return walkPhase;
    }

    /**
     * @return the last explicit stride strength in [0, 1]
     */
    public float walkStrength() {
        return walkStrength;
    }

    /**
     * Configures a typed callback for a pose owned by the current frame. The backend calls it after applying
     * the scene clock and locomotion. Use absolute animation time and sample the full pose on each call.
     *
     * @param stateType native render-state class to accept
     * @param sampler   updates the transient state when it matches stateType
     * @param <S>       native render-state type
     * @return this node
     * @throws NullPointerException if either argument is null
     */
    @WikiMethod(section = "<key:scene.api.section.animation>", sectionOrder = 30, summary = "<key:entity.node.api.set.native.animation.summary>")
    @WikiReturn("<key:scene.api.return.node>")
    @WikiThrows(type = NullPointerException.class, description = "<key:entity.node.api.set.native.animation.throws.NullPointerException>")
    @AiDoc(section = "Animation", summary = "Customize the transient vanilla state after the scene clock and gait have been applied.")
    public <S> EntityNode setNativeAnimation(
            @WikiParameter(description = "<key:entity.node.api.set.native.animation.param.state.type>") Class<S> stateType,
            @WikiParameter(description = "<key:entity.node.api.set.native.animation.param.sampler>") Consumer<? super S> sampler) {
        Objects.requireNonNull(stateType, "stateType");
        Objects.requireNonNull(sampler, "sampler");
        nativeAnimation = state -> {
            if (stateType.isInstance(state)) sampler.accept(stateType.cast(state));
        };
        invalidateContent();
        return this;
    }

    /**
     * Removes the native animation callback.
     *
     * @return this node
     */
    @WikiMethod(section = "<key:scene.api.section.animation>", sectionOrder = 30, summary = "<key:entity.node.api.clear.native.animation.summary>")
    @WikiReturn("<key:scene.api.return.node>")
    @AiDoc(section = "Animation", summary = "Removes the callback registered with setNativeAnimation.")
    public EntityNode clearNativeAnimation() {
        if (nativeAnimation != null) {
            nativeAnimation = null;
            invalidateContent();
        }
        return this;
    }

    /**
     * @return whether a native animation callback has been configured
     */
    public boolean hasNativeAnimation() {
        return nativeAnimation != null;
    }

    /**
     * @return whether a timeline action is active at the current sample
     */
    public boolean hasAction() {
        return action != null;
    }

    /**
     * Applies the timeline's selected action and invalidates cached rendering on a pose change.
     *
     * @param clip     selected clip, or null outside an action interval
     * @param progress normalized cycle phase in [0, 1)
     */
    void setTimelineAction(EntityActionClip<?> clip, float progress) {
        if (action != clip || actionProgress != progress) {
            action = clip;
            actionProgress = progress;
            invalidateContent();
        }
    }

    /**
     * Applies the user animation callback, then the selected timeline action, during backend rendering.
     *
     * @param state transient native render state that will be submitted for this node
     */
    public void sampleNativeAnimation(Object state) {
        if (nativeAnimation != null) nativeAnimation.accept(state);
        if (action != null) action.apply(state, actionProgress);
    }

    /**
     * Reads the entity type ID.
     *
     * @return the ID, or null for native input.
     */
    @WikiMethod(section = "Sources", sectionOrder = 10, order = 30,
            summary = "Reads the entity type ID.")
    @WikiReturn("the ID, or null for native input")
    @AiDoc(section = "Sources", sectionOrder = 10, order = 30,
            summary = "Reads the entity type ID.")
    public String typeId() {
        return typeId;
    }

    /**
     * Reads the native input object.
     *
     * @return the native input object, or null for a type ID.
     */
    @WikiMethod(section = "Sources", sectionOrder = 10, order = 40,
            summary = "Reads the native input object.")
    @WikiReturn("the native input object, or null for a type ID")
    @AiDoc(section = "Sources", sectionOrder = 10, order = 40,
            summary = "Reads the native input object.")
    public Object nativeSource() {
        return nativeSource;
    }

    /**
     * Resolves the entity for the renderer or a platform helper.
     *
     * @param platform platform adapter, or null.
     * @return the native entity, or null if unavailable or invalid.
     */
    @WikiMethod(section = "Resolution", sectionOrder = 30, order = 10,
            summary = "Resolves the entity for the renderer or a platform helper.")
    @WikiReturn("the native entity, or null if unavailable or invalid")
    @AiDoc(section = "Resolution", sectionOrder = 30, order = 10,
            summary = "Resolves the entity for the renderer or a platform helper.")
    public Object nativeEntity(
            @WikiParameter(description = "platform adapter, or null.") ScenePlatform platform) {
        return entity.get(platform, this::resolve, this, false);
    }

    /**
     * Resolves the entity and propagates resolution errors.
     *
     * @param platform platform adapter.
     * @return the native entity, or null if temporarily unavailable.
     * @throws IllegalArgumentException      if the type is unknown.
     * @throws UnsupportedOperationException if the platform cannot render scene entities.
     */
    @WikiMethod(section = "Resolution", sectionOrder = 30, order = 20,
            summary = "Resolves the entity and propagates resolution errors.")
    @WikiReturn("the native entity, or null if temporarily unavailable")
    @WikiThrows(type = IllegalArgumentException.class, description = "if the type is unknown.")
    @WikiThrows(type = UnsupportedOperationException.class, description = "if the platform cannot render scene entities.")
    @AiDoc(section = "Resolution", sectionOrder = 30, order = 20,
            summary = "Resolves the entity and propagates resolution errors.")
    public Object requireNativeEntity(
            @WikiParameter(description = "platform adapter.") ScenePlatform platform) {
        return entity.get(Objects.requireNonNull(platform, "platform"), this::resolve, this, true);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Uses the entity's dimensions when available and otherwise falls back to the node's custom mesh.</p>
     */
    @Override
    protected Aabb contentBounds() {
        ScenePlatform platform = ScenePlatform.current();
        Object resolved = nativeEntity(platform);
        Aabb bounds = resolved == null ? null : platform.entityBounds(resolved);
        return bounds != null ? bounds : super.contentBounds();
    }

    /**
     * Formats the source for logging and debugging.
     *
     * @return a description of the type ID or native source.
     */
    @Override
    public String toString() {
        return "EntityNode[" + (typeId != null ? typeId : nativeSource) + "]";
    }

    /**
     * Resolves the current source with the given platform.
     *
     * @param platform platform adapter.
     * @return the native entity, or null.
     */
    private Object resolve(ScenePlatform platform) {
        return typeId != null ? platform.resolveEntity(typeId) : platform.nativeEntity(nativeSource);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityNode setPosition(float x, float y, float z) {
        super.setPosition(x, y, z);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityNode setRotation(dev.sixik.unigui.api.math.QuatView value) {
        super.setRotation(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityNode setScale(float x, float y, float z) {
        super.setScale(x, y, z);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityNode setVisible(boolean visible) {
        super.setVisible(visible);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityNode setOrigin(float x, float y, float z) {
        super.setOrigin(x, y, z);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityNode centerOrigin() {
        super.centerOrigin();
        return this;
    }
}
