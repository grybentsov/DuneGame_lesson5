package com.dune.game.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public class Tank extends GameObject implements Poolable {
    public enum Owner {
        PLAYER, AI
    }

    private Owner ownerType;
    private Weapon weapon;
    private Vector2 destination;
    private TextureRegion[] textures;
    private TextureRegion[] weaponsTextures;

    private TextureRegion progressbarTexture;
    private int hp;
    private int hpMax;
    private float angle;
    private float speed;
    private float rotationSpeed;

    private float moveTimer;
    private float lifeTime;
    private float timePerFrame;
    private int container;
    private Tank target;

    public Weapon getWeapon() {
        return weapon;
    }

    public void moveBy(Vector2 value) {
        boolean stayStill = false;          // не знаем, стоял ли танк спокойно или нет
        if (position.dst(destination) < 3.0f) {
            stayStill = true;               // считаем, что стоит спокойно
        }
        position.add(value);
        if (stayStill) {        // если стоял на месте до этого
            destination.set(position);      // точка назначения перемещается за текущей позицией
        }
    }

    public Owner getOwnerType() {
        return ownerType;
    }

    @Override
    public boolean isActive() {
        return hp > 0;
    }

    public Tank(GameController gc) {
        super(gc);
        this.progressbarTexture = Assets.getInstance().getAtlas().findRegion("progressbar");
        this.weaponsTextures = new TextureRegion[]{
                Assets.getInstance().getAtlas().findRegion("turret"),
                Assets.getInstance().getAtlas().findRegion("harvester")
        };

        this.timePerFrame = 0.08f;
        this.rotationSpeed = 90.0f;
    }

    public void setup(Owner ownerType, float x, float y) {
        this.textures = Assets.getInstance().getAtlas().findRegion("tankcore").split(64, 64)[0];
        this.position.set(x, y);
        this.ownerType = ownerType;
        this.speed = 120.0f;
        this.hpMax = 100;
        this.hp = this.hpMax;
        if (MathUtils.random() < 0.5f) {
            this.weapon = new Weapon(Weapon.Type.HARVEST, 3.0f, 1);
        } else {
            this.weapon = new Weapon(Weapon.Type.GROUND, 1.5f, 1);
        }
        this.destination = new Vector2(position);
    }

    private int getCurrentFrameIndex() {
        return (int) (moveTimer / timePerFrame) % textures.length;
    }

    public boolean takeDamage (int damage){
        hp -= damage;
        if (hp <= 0){
            return true;
        }
        return false;
    }

    public void update(float dt) {
        lifeTime += dt;                                     // время жизни от начала игры
        // Если у танка есть цель, он пытается ее атаковать
        if (target != null) {
            destination.set(target.position);              // устанавливаем точку назначения там, где цель
            if (position.dst(target.position) < 240.0f) { // если расстояние между нами и целью меньше 240 пикселей,
                destination.set(position);                 // то мы останавливаемся на месте
            }
        }
        // Если танку необходимо доехать до какой-то точки, он работает в этом условии
        if (position.dst(destination) > 3.0f) {
            float angleTo = tmp.set(destination).sub(position).angle();
            angle = rotateTo(angle, angleTo, rotationSpeed, dt);
            moveTimer += dt;
            tmp.set(speed, 0).rotate(angle);
            position.mulAdd(tmp, dt);
            if (position.dst(destination) < 120.0f && Math.abs(angleTo - angle) > 10) {
                position.mulAdd(tmp, -dt);
            }
        }
        updateWeapon(dt);
        checkBounds();
    }

    public void commandMoveTo(Vector2 point) {
        destination.set(point);
    }

    public void commandAttack(Tank target) {
        this.target = target;
    }

    public void updateWeapon(float dt) {
        if (weapon.getType() == Weapon.Type.GROUND && target != null) {                 // если боевой танк и есть есть мишень, то
            float angleTo = tmp.set(target.position).sub(position).angle();             // рассчитывает угол и поворачивается в
            weapon.setAngle(rotateTo(weapon.getAngle(), angleTo, 180.0f, dt));    // ту сторону, где враг,
            int power = weapon.use(dt);                                                     // и применяет оружие
            if (power > -1) {                                                           // формируем снаряд
                gc.getProjectilesController().setup(position, weapon.getAngle());       // и снаряд полетел
                gc.targetHitCheck(dt);
                target.takeDamage(25);
                if (hp == 0) {
                        target = null;
                    }
            }
 //           gc.targetHitCheck(dt);
//                if (position.dst(target.position) < 30){
//                    gc.getProjectilesController().deactivate();
//            }
        }
        if (weapon.getType() == Weapon.Type.HARVEST) {
            if (gc.getMap().getResourceCount(this) > 0) {
                int result = weapon.use(dt);                                                // применяет снаряжение харвестера
                if (result > -1) {
                    container += gc.getMap().harvestResource(this, result);
                }
            } else {
                weapon.reset();
            }
        }
    }



    public void checkBounds() {
        if (position.x < 40) {
            position.x = 40;
        }
        if (position.y < 40) {
            position.y = 40;
        }
        if (position.x > 1240) {
            position.x = 1240;
        }
        if (position.y > 680) {
            position.y = 680;
        }
    }

    public void render(SpriteBatch batch) {
        if (gc.isTankSelected(this)) {          // если танк выбран
            float c = 0.7f + (float) Math.sin(lifeTime * 8.0f) * 0.3f;  // то цвет его будет немного отличаться
            batch.setColor(c, c, c, 1.0f);
        }
        batch.draw(textures[getCurrentFrameIndex()], position.x - 40, position.y - 40, 40, 40, 80, 80, 1, 1, angle);
        batch.draw(weaponsTextures[weapon.getType().getImageIndex()], position.x - 40, position.y - 40, 40, 40, 80, 80, 1, 1, weapon.getAngle());

        batch.setColor(1, 1, 1, 1);         // сбрасываем все перенастройки на стандартный белый цвет
        if (weapon.getType() == Weapon.Type.HARVEST && weapon.getUsageTimePercentage() > 0.0f) {
            batch.setColor(0.2f, 0.2f, 0.0f, 1.0f);
            batch.draw(progressbarTexture, position.x - 32, position.y + 30, 64, 12);
            batch.setColor(1.0f, 1.0f, 0.0f, 1.0f);
            batch.draw(progressbarTexture, position.x - 30, position.y + 32, 60 * weapon.getUsageTimePercentage(), 8);
            batch.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }
// служебный метод - поворот (турели), изменение угла
    public float rotateTo(float srcAngle, float angleTo, float rSpeed, float dt) {
        if (Math.abs(srcAngle - angleTo) > 3.0f) {
            if ((srcAngle > angleTo && Math.abs(srcAngle - angleTo) <= 180.0f) || (srcAngle < angleTo && Math.abs(srcAngle - angleTo) > 180.0f)) {
                srcAngle -= rSpeed * dt;
            } else {
                srcAngle += rSpeed * dt;
            }
        }
        if (srcAngle < 0.0f) {
            srcAngle += 360.0f;
        }
        if (srcAngle > 360.0f) {
            srcAngle -= 360.0f;
        }
        return srcAngle;        // возвращается новое значение угла после поворота
    }
}