package io.github.some_example_name;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;

public class GameScreen implements Screen {
    private final Main game;
    private PerspectiveCamera cam;
    private PerspectiveCamera gunCam;
    private ModelBatch modelBatch;
    private Environment environment;

    private int score = 0;
    private int hp = 100;
    private boolean isGameOver = false;

    private Texture grassTex, stoneTex, woodTex, cloudTex;

    private Model floorModel;
    private Model wallModel;
    private Model enemyModel;
    private Model fastEnemyModel;
    private Model boxModel;
    private Model gunPartModel;
    private Model decorModel;

    private Array<GameObject> walls = new Array<>();
    private Array<GameObject> enemies = new Array<>();
    private Array<GameObject> boxes = new Array<>();
    private Array<ModelInstance> decors = new Array<>(); // Летающие декорации
    private Array<GameObject> allRenderObjects = new Array<>();

    private ModelInstance gunBase;
    private ModelInstance gunBarrel;

    private Vector3 playerPos = new Vector3(0, 1.5f, 0);
    private Vector3 playerVelocity = new Vector3();
    private final float SPEED = 7f;
    private float currentSpeed = 7f;
    private final float GRAVITY = -22f;
    private final float JUMP_FORCE = 8f;
    private boolean isGrounded = true;

    private float pitch = 0;
    private float yaw = 0;
    private final float MOUSE_SENSITIVITY = 0.15f;
    private final float ENEMY_SPEED = 2.2f;
    private float spawnTimer = 0;
    private final float SPAWN_COOLDOWN = 2.5f;
    private float boxSpawnTimer = 0f;
    private final float BOX_SPAWN_COOLDOWN = 12.0f;

    private float recoil = 0f;
    private float dashTimer = 0f;
    private final float DASH_COOLDOWN = 1.0f;

    private static class GameObject {
        ModelInstance instance;
        int hp;
        boolean isFast;
        Vector3 pos = new Vector3();
        float radius;

        GameObject(Model model, float x, float y, float z, int hp, boolean isFast, float radius) {
            this.instance = new ModelInstance(model, x, y, z);
            this.hp = hp;
            this.isFast = isFast;
            this.radius = radius;
            this.instance.transform.getTranslation(this.pos);
        }
        void updatePos() { this.instance.transform.getTranslation(this.pos); }
    }

    public GameScreen(Main game) {
        this.game = game;
        modelBatch = new ModelBatch();
        ModelBuilder modelBuilder = new ModelBuilder();
        Gdx.input.setCursorCatched(true);

        grassTex = new Texture(Gdx.files.internal("grass.png"));
        stoneTex = new Texture(Gdx.files.internal("stone.png"));
        woodTex = new Texture(Gdx.files.internal("wood.png"));
        cloudTex = new Texture(Gdx.files.internal("cloud.png"));

        cam = new PerspectiveCamera(75, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.position.set(playerPos);
        cam.lookAt(0, 1.5f, -1);
        cam.near = 0.1f;
        cam.far = 300f;
        cam.update();

        gunCam = new PerspectiveCamera(75, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        gunCam.position.set(0, 0, 0);
        gunCam.lookAt(0, 0, -1);
        gunCam.near = 0.1f;
        gunCam.far = 10f;
        gunCam.update();

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.5f, 0.5f, 0.5f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        floorModel = modelBuilder.createBox(80f, 1f, 80f, new Material(TextureAttribute.createDiffuse(grassTex)), Usage.Position | Usage.Normal | Usage.TextureCoordinates);
        wallModel = modelBuilder.createBox(2f, 5f, 2f, new Material(TextureAttribute.createDiffuse(stoneTex)), Usage.Position | Usage.Normal | Usage.TextureCoordinates);
        boxModel = modelBuilder.createBox(1.5f, 1.5f, 1.5f, new Material(TextureAttribute.createDiffuse(woodTex)), Usage.Position | Usage.Normal | Usage.TextureCoordinates);
        decorModel = modelBuilder.createBox(6f, 2f, 4f, new Material(TextureAttribute.createDiffuse(cloudTex)), Usage.Position | Usage.Normal | Usage.TextureCoordinates);

        enemyModel = modelBuilder.createBox(1.5f, 1.5f, 1.5f, new Material(ColorAttribute.createDiffuse(Color.GOLD)), Usage.Position | Usage.Normal);
        fastEnemyModel = modelBuilder.createBox(0.8f, 0.8f, 0.8f, new Material(ColorAttribute.createDiffuse(Color.FIREBRICK)), Usage.Position | Usage.Normal);
        gunPartModel = modelBuilder.createBox(1f, 1f, 1f, new Material(ColorAttribute.createDiffuse(Color.DARK_GRAY)), Usage.Position | Usage.Normal);

        allRenderObjects.add(new GameObject(floorModel, 0, -0.5f, 0, 1, false, 40f));
        walls.add(new GameObject(wallModel, -5, 2.5f, -5, 1, false, 1.4f));
        walls.add(new GameObject(wallModel, 5, 2.5f, -3, 1, false, 1.4f));
        walls.add(new GameObject(wallModel, 0, 2.5f, -10, 1, false, 1.4f));
        for (GameObject wall : walls) { allRenderObjects.add(wall); }

        boxes.add(new GameObject(boxModel, -3, 0.75f, -2, 2, false, 1.3f));
        boxes.add(new GameObject(boxModel, 3, 0.75f, -5, 2, false, 1.3f));
        boxes.add(new GameObject(boxModel, 2, 0.75f, -2, 2, false, 1.3f));
        for (GameObject box : boxes) { allRenderObjects.add(box); }

        for (int i = 0; i < 15; i++) {
            float cx = MathUtils.random(-35f, 35f);
            float cy = MathUtils.random(15f, 25f); // Высота в небе
            float cz = MathUtils.random(-35f, 35f);
            decors.add(new ModelInstance(decorModel, cx, cy, cz));
        }

        gunBase = new ModelInstance(gunPartModel);
        gunBarrel = new ModelInstance(gunPartModel);

        spawnEnemy(); spawnEnemy();
    }

    private void spawnEnemy() {
        float angle = MathUtils.random(0, MathUtils.PI2);
        float distance = MathUtils.random(15f, 30f);
        float x = MathUtils.clamp(playerPos.x + MathUtils.cos(angle) * distance, -38f, 38f);
        float z = MathUtils.clamp(playerPos.z + MathUtils.sin(angle) * distance, -38f, 38f);

        boolean isFast = MathUtils.randomBoolean(0.4f);
        GameObject enemy = isFast ? new GameObject(fastEnemyModel, x, 0.4f, z, 1, true, 0.6f) : new GameObject(enemyModel, x, 0.75f, z, 2, false, 1.1f);
        enemies.add(enemy);
        allRenderObjects.add(enemy);
    }

    @Override
    public void render(float delta) {
        if (isGameOver) {
            if (Gdx.input.isKeyPressed(Input.Keys.R)) {
                hp = 100; score = 0; playerPos.set(0, 1.5f, 0);
                for (GameObject enemy : enemies) { allRenderObjects.removeValue(enemy, true); }
                enemies.clear(); spawnEnemy(); spawnEnemy();
                isGameOver = false; Gdx.input.setCursorCatched(true);
            }
            drawGameOverScreen();
            return;
        }

        spawnTimer += delta;
        if (spawnTimer >= SPAWN_COOLDOWN) { spawnEnemy(); spawnTimer = 0; }

        boxSpawnTimer += delta;
        if (boxSpawnTimer >= BOX_SPAWN_COOLDOWN) {
            float angle = MathUtils.random(0, MathUtils.PI2);
            float distance = MathUtils.random(5f, 20f);
            float bx = MathUtils.clamp(playerPos.x + MathUtils.cos(angle) * distance, -38f, 38f);
            float bz = MathUtils.clamp(playerPos.z + MathUtils.sin(angle) * distance, -38f, 38f);
            if (Vector3.dst(bx, 0.75f, bz, playerPos.x, playerPos.y, playerPos.z) > 4f) {
                GameObject newBox = new GameObject(boxModel, bx, 0.75f, bz, 2, false, 1.3f);
                boxes.add(newBox);
                allRenderObjects.add(newBox);
            }
            boxSpawnTimer = 0f;
        }

        if (recoil > 0) { recoil -= delta * 5f; if (recoil < 0) recoil = 0; }
        if (dashTimer > 0) dashTimer -= delta;

        if (Gdx.input.isCursorCatched()) {
            float mouseX = -Gdx.input.getDeltaX() * MOUSE_SENSITIVITY;
            float mouseY = -Gdx.input.getDeltaY() * MOUSE_SENSITIVITY;
            yaw += mouseX;
            cam.direction.rotate(Vector3.Y, mouseX);
            if (pitch + mouseY > -85 && pitch + mouseY < 85) {
                pitch += mouseY;
                Vector3 right = cam.direction.cpy().crs(cam.up).nor();
                cam.direction.rotate(right, mouseY);
            }
        }

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && Gdx.input.isCursorCatched()) {
            recoil = 0.15f;
            Ray ray = cam.getPickRay(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f);
            GameObject hitTarget = null; float closestDistance = Float.MAX_VALUE; Vector3 intersection = new Vector3();

            for (GameObject enemy : enemies) {
                if (Intersector.intersectRaySphere(ray, enemy.pos, enemy.radius, intersection)) {
                    float dist = ray.origin.dst(intersection);
                    if (dist < closestDistance) { closestDistance = dist; hitTarget = enemy; }
                }
            }
            for (GameObject box : boxes) {
                if (Intersector.intersectRaySphere(ray, box.pos, box.radius, intersection)) {
                    float dist = ray.origin.dst(intersection);
                    if (dist < closestDistance) { closestDistance = dist; hitTarget = box; }
                }
            }
            if (hitTarget != null) {
                hitTarget.hp--;
                if (hitTarget.hp <= 0) {
                    if (enemies.contains(hitTarget, true)) { enemies.removeValue(hitTarget, true); score++; }
                    else { boxes.removeValue(hitTarget, true); hp = Math.min(100, hp + 20); }
                    allRenderObjects.removeValue(hitTarget, true);
                }
            }
        }
        Vector3 oldPos = playerPos.cpy();
        Vector3 moveDirection = new Vector3();
        Vector3 forward = cam.direction.cpy(); forward.y = 0; forward.nor();
        Vector3 rightDir = cam.direction.cpy().crs(cam.up); rightDir.y = 0; rightDir.nor();
        if (Gdx.input.isKeyPressed(Input.Keys.W)) moveDirection.add(forward);
        if (Gdx.input.isKeyPressed(Input.Keys.S)) moveDirection.add(forward.scl(-1));
        if (Gdx.input.isKeyPressed(Input.Keys.A)) moveDirection.add(rightDir.scl(-1));
        if (Gdx.input.isKeyPressed(Input.Keys.D)) moveDirection.add(rightDir);
        moveDirection.nor();
        if (Gdx.input.isKeyPressed(Input.Keys.SPACE) && isGrounded) {
            playerVelocity.y = JUMP_FORCE;
            isGrounded = false;
            currentSpeed = Math.min(22f, currentSpeed * 1.2f);
        }
        if (isGrounded && !Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
            currentSpeed = Math.max(7f, currentSpeed - delta * 15f);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT) && dashTimer <= 0) {
            Vector3 dashDirection = moveDirection.isZero() ? forward : moveDirection;
            playerPos.add(dashDirection.scl(5.0f)); dashTimer = DASH_COOLDOWN;
        } else {
            playerPos.x += moveDirection.x * currentSpeed * delta;
            if (checkCollisions()) playerPos.x = oldPos.x;
            playerPos.z += moveDirection.z * currentSpeed * delta;
            if (checkCollisions()) playerPos.z = oldPos.z;
        }
        playerPos.x = MathUtils.clamp(playerPos.x, -39f, 39f);
        playerPos.z = MathUtils.clamp(playerPos.z, -39f, 39f);
        if (!isGrounded) playerVelocity.y += GRAVITY * delta;
        playerPos.y += playerVelocity.y * delta;
        if (playerPos.y <= 1.5f) {
            playerPos.y = 1.5f; playerVelocity.y = 0; isGrounded = true;
            if (!Gdx.input.isKeyPressed(Input.Keys.SPACE)) currentSpeed = 7f;
        }
        cam.position.set(playerPos); cam.update();
        for (int i = enemies.size - 1; i >= 0; i--) {
            GameObject enemy = enemies.get(i);
            Vector3 toPlayer = playerPos.cpy().sub(enemy.pos); toPlayer.y = 0; toPlayer.nor();
            Vector3 finalDirection = toPlayer.cpy();
            float enemyCurrentSpeed = enemy.isFast ? (ENEMY_SPEED * 2.2f) : ENEMY_SPEED;
            for (GameObject wall : walls) {
                if (enemy.pos.dst(wall.pos) < 3.5f) {
                    Vector3 toWall = wall.pos.cpy().sub(enemy.pos); toWall.y = 0;
                    if (toPlayer.dot(toWall.nor()) > 0.4f) {
                        Vector3 avoid = new Vector3(-toWall.z, 0, toWall.x).nor();
                        if (avoid.dot(toPlayer) < 0) avoid.scl(-1);
                        finalDirection.add(avoid.scl(2.0f)).nor();
                    }
                }
            }
            for (GameObject box : boxes) {
                if (enemy.pos.dst(box.pos) < 3.0f) {
                    Vector3 toBox = box.pos.cpy().sub(enemy.pos); toBox.y = 0;
                    if (toPlayer.dot(toBox.nor()) > 0.4f) {
                        Vector3 avoid = new Vector3(-toBox.z, 0, toBox.x).nor();
                        if (avoid.dot(toPlayer) < 0) avoid.scl(-1);
                        finalDirection.add(avoid.scl(2.0f)).nor();
                    }
                }
            }
            for (GameObject other : enemies) {
                if (other == enemy) continue;
                if (enemy.pos.dst(other.pos) < 2.0f) {
                    Vector3 pushAway = enemy.pos.cpy().sub(other.pos); pushAway.y = 0; pushAway.nor();
                    finalDirection.add(pushAway.scl(1.5f)).nor();
                }
            }
            Vector3 oldEnemyPos = enemy.pos.cpy();
            enemy.pos.x += finalDirection.x * enemyCurrentSpeed * delta;
            enemy.instance.transform.setToTranslation(enemy.pos); enemy.updatePos();
            if (checkEnemyCollisions(enemy)) enemy.pos.x = oldEnemyPos.x;
            enemy.pos.z += finalDirection.z * enemyCurrentSpeed * delta;
            enemy.instance.transform.setToTranslation(enemy.pos); enemy.updatePos();
            if (checkEnemyCollisions(enemy)) enemy.pos.z = oldEnemyPos.z;
            enemy.pos.x = MathUtils.clamp(enemy.pos.x, -39f, 39f);
            enemy.pos.z = MathUtils.clamp(enemy.pos.z, -39f, 39f);
            enemy.instance.transform.setToTranslation(enemy.pos); enemy.updatePos();
            if (playerPos.dst(enemy.pos) < 1.6f) {
                hp -= enemy.isFast ? 15 : 25;
                enemies.removeValue(enemy, true); allRenderObjects.removeValue(enemy, true);
                if (hp <= 0) { hp = 0; isGameOver = true; Gdx.input.setCursorCatched(false); }
            }
        }
        float aspectRatio = (float)Gdx.graphics.getWidth() / Gdx.graphics.getHeight();
        float xOffset = 0.22f * aspectRatio;
        gunBase.transform.setToTranslation(xOffset, -0.25f, -0.6f + recoil);
        gunBase.transform.setFromEulerAngles(0, 0, 0);
        gunBase.transform.scale(0.08f, 0.18f, 0.08f);
        gunBarrel.transform.setToTranslation(xOffset, -0.18f, -0.75f + recoil);
        gunBarrel.transform.setFromEulerAngles(0, 0, 0);
        gunBarrel.transform.scale(0.08f, 0.08f, 0.25f);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) game.setScreen(new MainMenuScreen(game));
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.53f, 0.81f, 0.92f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        modelBatch.begin(cam);
        for (GameObject go : allRenderObjects) { modelBatch.render(go.instance, environment); }
        for (ModelInstance cloud : decors) { modelBatch.render(cloud, environment); }
        modelBatch.end();
        Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
        modelBatch.begin(gunCam);
        modelBatch.render(gunBase, environment);
        modelBatch.render(gunBarrel, environment);
        modelBatch.end();
        game.batch.getProjectionMatrix().setToOrtho2D(0, 0, 1920, 1080);
        game.batch.begin();
        game.font.setColor(Color.WHITE);
        game.font.draw(game.batch, "KILLS: " + score, 40, 1080 - 40);
        game.font.setColor(Color.RED);
        game.font.draw(game.batch, "HP: " + hp, 40, 1080 - 90);
        game.font.setColor(Color.CYAN);
        game.font.draw(game.batch, "DASH: " + (dashTimer <= 0 ? "READY" : String.format("%.1f", dashTimer)), 40, 1080 - 140);
        game.font.setColor(Color.YELLOW);
        game.font.draw(game.batch, "CONTROLS: WASD - Move | SHIFT - Dash | SPACE - BunnyHop | LMB - Shoot | ESC - Menu", 350, 50);
        game.font.setColor(Color.WHITE);
        game.font.draw(game.batch, "+", 1920 / 2f - 8, 1080 / 2f + 12);
        game.batch.end();
    }
    private boolean checkCollisions() {
        for (GameObject wall : walls) { if (Math.abs(playerPos.x - wall.pos.x) < wall.radius && Math.abs(playerPos.z - wall.pos.z) < wall.radius) return true; }
        for (GameObject box : boxes) { if (Math.abs(playerPos.x - box.pos.x) < box.radius && Math.abs(playerPos.z - box.pos.z) < box.radius) return true; }
        return false;
    }
    private boolean checkEnemyCollisions(GameObject currentEnemy) {
        for (GameObject wall : walls) {
            if (Math.abs(currentEnemy.pos.x - wall.pos.x) < (currentEnemy.radius + wall.radius - 0.4f) &&
                Math.abs(currentEnemy.pos.z - wall.pos.z) < (currentEnemy.radius + wall.radius - 0.4f)) return true;
        }
        for (GameObject box : boxes) {
            if (box == currentEnemy) continue;
            if (Math.abs(currentEnemy.pos.x - box.pos.x) < (currentEnemy.radius + box.radius - 0.4f) &&
                Math.abs(currentEnemy.pos.z - box.pos.z) < (currentEnemy.radius + box.radius - 0.4f)) return true;
        }
        return false;
    }
    private void drawGameOverScreen() {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        game.batch.begin();
        game.font.setColor(Color.RED);
        game.font.draw(game.batch, "GAME OVER", Gdx.graphics.getWidth() / 2f - 100, Gdx.graphics.getHeight() / 2f + 50);
        game.font.setColor(Color.WHITE);
        game.font.draw(game.batch, "TOTAL KILLS: " + score, Gdx.graphics.getWidth() / 2f - 110, Gdx.graphics.getHeight() / 2f);
        game.font.draw(game.batch, "Press 'R' to Restart", Gdx.graphics.getWidth() / 2f - 130, Gdx.graphics.getHeight() / 2f - 50);
        game.batch.end();
    }
    @Override public void show() {}
    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {
        modelBatch.dispose();
        grassTex.dispose(); stoneTex.dispose(); woodTex.dispose(); cloudTex.dispose();
        floorModel.dispose(); wallModel.dispose(); enemyModel.dispose();
        fastEnemyModel.dispose(); boxModel.dispose(); gunPartModel.dispose(); decorModel.dispose();
    }
}
