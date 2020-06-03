package com.dune.game.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameController {
    private BattleMap map;
    private ProjectilesController projectilesController;
    private TanksController tanksController;
    private Vector2 tmp;
    private Vector2 selectionStart;

    private List<Tank> selectedUnits;

    public TanksController getTanksController() {
        return tanksController;
    }

    public List<Tank> getSelectedUnits() {
        return selectedUnits;
    }

    public ProjectilesController getProjectilesController() {
        return projectilesController;
    }

    public BattleMap getMap() {
        return map;
    }

    // Инициализация игровой логики
    public GameController() {
        Assets.getInstance().loadAssets();
        this.tmp = new Vector2();
        this.selectionStart = new Vector2();
        this.selectedUnits = new ArrayList<>();
        this.map = new BattleMap();
        this.projectilesController = new ProjectilesController(this);
        this.tanksController = new TanksController(this);
        for (int i = 0; i < 5; i++) {
            this.tanksController.setup(MathUtils.random(80, 1200), MathUtils.random(80, 640), Tank.Owner.PLAYER);
        }
        for (int i = 0; i < 2; i++) {
            this.tanksController.setup(MathUtils.random(80, 1200), MathUtils.random(80, 640), Tank.Owner.AI);
        }
        prepareInput();
    }

    public void update(float dt) {
        tanksController.update(dt);
        projectilesController.update(dt);
        targetHitCheck(dt);                 // проверка попадания в цель
        map.update(dt);
        checkCollisions(dt);
        // checkSelection();
    }
// проверка столкновений пар танков и их разъезд
    public void checkCollisions(float dt) {
        for (int i = 0; i < tanksController.activeSize() - 1; i++) {
            Tank t1 = tanksController.getActiveList().get(i);
            for (int j = i + 1; j < tanksController.activeSize(); j++) {
                Tank t2 = tanksController.getActiveList().get(j);
                float dst = t1.getPosition().dst(t2.getPosition());
                if (dst < 30 + 30) {
                    float colLengthD2 = (60 - dst) / 2;
                    tmp.set(t2.getPosition()).sub(t1.getPosition()).nor().scl(colLengthD2);
                    t2.moveBy(tmp);
                    tmp.scl(-1);
                    t1.moveBy(tmp);
                }
            }
        }
    }

    public void targetHitCheck(float dt){
        for (int i = 0; i < projectilesController.activeSize(); i++) {
            Projectile p1 = projectilesController.getActiveList().get(i);
            for (int j = 0; j < tanksController.activeSize(); j++) {
                Tank t = tanksController.getActiveList().get(j);
                if (p1.position.dst(t.getPosition()) < 30) {
                projectilesController.getActiveList().get(i).deactivate();
                }
            }
        }
    }

    public boolean isTankSelected(Tank tank) {
        return selectedUnits.contains(tank);  // список selectedUnits содержит этот танк
    }

    public void prepareInput() {
        InputProcessor ip = new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {  // если начали зажимать
                if (button == Input.Buttons.LEFT) {
                    selectionStart.set(screenX, 720 - screenY); // selectionStart устанавливается в эту точку
                }
                return true;        // этот клик обработан
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) { // как только кнопку отпускаем
                if (button == Input.Buttons.LEFT) {                                           // если нажата левая кнопка,
                    tmp.set(Gdx.input.getX(), 720 - Gdx.input.getY());              // то в tmp кладем координаты
// это чтобы массовый выбор работал слева направо
                    if (tmp.x < selectionStart.x) {        // если tmp.x оказался левее selectionStart.x,
                        float t = tmp.x;                    // то меняем их местами
                        tmp.x = selectionStart.x;
                        selectionStart.x = t;
                    }
                    if (tmp.y > selectionStart.y) {
                        float t = tmp.y;
                        tmp.y = selectionStart.y;
                        selectionStart.y = t;
                    }

                    selectedUnits.clear();                                                                      // очищаем список
                    if (Math.abs(tmp.x - selectionStart.x) > 20 & Math.abs(tmp.y - selectionStart.y) > 20) {  // тогда это масовый выбор танков
                        for (int i = 0; i < tanksController.getActiveList().size(); i++) {          // перебираем танки
                            Tank t = tanksController.getActiveList().get(i);
                            if (t.getOwnerType() == Tank.Owner.PLAYER && t.getPosition().x > selectionStart.x && t.getPosition().x < tmp.x
                                    && t.getPosition().y > tmp.y && t.getPosition().y < selectionStart.y
                            ) {
                                selectedUnits.add(t);                                                   // тогда этот объект выделяем
                            }
                        }
                    } else {                                                                        // иначе - обычный выбор танка
                        for (int i = 0; i < tanksController.getActiveList().size(); i++) {
                            Tank t = tanksController.getActiveList().get(i);
                            if (t.getPosition().dst(tmp) < 30.0f) {                 // if (спрашиваем позицию танка и расстояние до нашего tmp-вектора) < 30 пикселей
                                selectedUnits.add(t);           // танк выбран (т.е. добавлен в selectedUnits)
                            }
                        }
                    }
                }
                return true;
            }
        };
        Gdx.input.setInputProcessor(ip);                    // приводим в действие этот InputProcessor
    }
}
