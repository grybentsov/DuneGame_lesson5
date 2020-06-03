package com.dune.game.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

public class TanksController extends ObjectPool<Tank> {
    private GameController gc;
    private Vector2 tmp;

    @Override
    protected Tank newObject() {
        return new Tank(gc);
    }

    public TanksController(GameController gc) {
        this.gc = gc;
        this.tmp = new Vector2();
    }

    public void render(SpriteBatch batch) {
        for (int i = 0; i < activeList.size(); i++) {
            activeList.get(i).render(batch);
        }
    }

    public void setup(float x, float y, Tank.Owner ownerType) {
        Tank t = activateObject();
        t.setup(ownerType, x, y);
    }

    public Tank getNearestAiTank(Vector2 point) {
        for (int i = 0; i < activeList.size(); i++) {
            Tank t = activeList.get(i);
            if (t.getOwnerType() == Tank.Owner.AI && t.getPosition().dst(point) < 30) {   // t.getPosition().dst(point) - расстояние между танком и точкой назначения
                return t;
            }
        }
        return null;
    }

    public void update(float dt) {
        for (int i = 0; i < activeList.size(); i++) {
            activeList.get(i).update(dt);
        }
        playerUpdate(dt);           // проверяем, что хочет сдлеать игрок
        aiUpdate(dt);               // и что хочет сделать AI
        checkPool();
    }
// логика игрока
    public void playerUpdate(float dt) {
        if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {           // если нажали правую кнопку
            for (int i = 0; i < gc.getSelectedUnits().size(); i++) {    // в SelectedUnits может попасть и бот, поэтому ниже проверяем, что танк наш
                Tank t = gc.getSelectedUnits().get(i);
                if (t.getOwnerType() == Tank.Owner.PLAYER && gc.getSelectedUnits().contains(t)) { // если танк наш и есть в списке выбранных
                    tmp.set(Gdx.input.getX(), 720 - Gdx.input.getY());                          // то находим точку, куда мы ткнули
                    if (t.getWeapon().getType() == Weapon.Type.HARVEST) {                       //если это харвестер, то
                        t.commandMoveTo(tmp);                                       // даем ему команду на движение в точку tmp
                    }
                    if (t.getWeapon().getType() == Weapon.Type.GROUND) {            // если это боевой танк,
                        Tank aiTank = gc.getTanksController().getNearestAiTank(tmp); // то смотрим, кликнули ли мы в какого-нибудь бота
                        if (aiTank == null) {                                          // если там бота нет,
                            t.commandMoveTo(tmp);               // то даем ему команду просто двигаться
                        } else {                            // иначе (если там бот),
                            t.commandAttack(aiTank);        // то даем команду атаковать
                        }
                    }
                }
            }
        }
    }

    public void aiUpdate(float dt) {

    }
}
