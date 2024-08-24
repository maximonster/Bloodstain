package com.Bloodstain;
// Code adapted from https://github.com/Mrnice98/Fake-Pet-Plugin
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.geometry.SimplePolygon;
import net.runelite.api.model.Jarvis;

import java.util.Random;

import static net.runelite.api.Perspective.COSINE;
import static net.runelite.api.Perspective.SINE;

public class Bloodstain {



    private Client client;
    private RuneLiteObject rlObject;
    private WorldPoint worldPoint;

    private final int[] duckModelIds = {26873, 26870};

    private String Playername;

    SimplePolygon clickbox;

    public void init(Client client, WorldPoint worldPoint)
    {
        this.client = client;
        this.rlObject = client.createRuneLiteObject();
        this.worldPoint = worldPoint;
    }




    public void setModel(Model model)
    {
        rlObject.setModel(model);
    }

    public RuneLiteObject getRlObject(){
        return rlObject;
    }

    public String getPlayerName(){
        return Playername;
    }

    public void spawn(WorldPoint position, int jauOrientation)
    {
        LocalPoint localPosition = LocalPoint.fromWorld(client, worldPoint);
        if (localPosition != null && client.getPlane() == position.getPlane()){
            rlObject.setLocation(localPosition, position.getPlane());
        }
        else {
            return;
        }
        rlObject.setOrientation(jauOrientation);
        rlObject.setActive(true);

    }

    public void despawn()
    {
        rlObject.setActive(false);
    }

    public LocalPoint getLocalLocation()
    {
        return rlObject.getLocation();
    }

    public boolean isActive()
    {
        return rlObject.isActive();
    }

    public int getOrientation()
    {
        return rlObject.getOrientation();
    }

    public SimplePolygon getClickbox(){
        return clickbox;
    }



    public String getExamine(String menuTarget){

        String BloodstainExamine = "{{playername}} died here ";
        String playername = menuTarget.split("'")[0];
        BloodstainExamine.replace("{{playername}}",playername);
        return BloodstainExamine;
    }


    public void onClientTick()
    {

        if (rlObject.isActive())
        {
                int targetPlane = worldPoint.getPlane();

                LocalPoint targetPosition = LocalPoint.fromWorld(client,worldPoint);

                if (targetPosition == null){
                    despawn();
                    return;
                }

                double intx = rlObject.getLocation().getX() - targetPosition.getX();
                double inty = rlObject.getLocation().getY() - targetPosition.getY();

                boolean rotationDone = rotateObject(intx,inty);

                if (client.getPlane() != targetPlane || !targetPosition.isInScene())
                {
                    // this actor is no longer in a visible area on our client, so let's despawn it
                    despawn();
                    return;
                }

            LocalPoint lp = getLocalLocation();
            int zOff = Perspective.getTileHeight(client, lp, client.getPlane());

            clickbox = calculateAABB(client, getRlObject().getModel(), getOrientation(), lp.getX(), lp.getY(), client.getPlane(), zOff);

        }

    }

    public boolean rotateObject(double intx, double inty)
    {

        final int JAU_FULL_ROTATION = 2048;
        int targetOrientation = radToJau(Math.atan2(intx, inty));
        int currentOrientation = rlObject.getOrientation();

        int dJau = (targetOrientation - currentOrientation) % JAU_FULL_ROTATION;

        if (dJau != 0)
        {
            final int JAU_HALF_ROTATION = 1024;
            final int JAU_TURN_SPEED = 32;
            int dJauCW = Math.abs(dJau);

            if (dJauCW > JAU_HALF_ROTATION)// use the shortest turn
            {
                dJau = (currentOrientation - targetOrientation) % JAU_FULL_ROTATION;
            }

            else if (dJauCW == JAU_HALF_ROTATION)// always turn right when turning around
            {
                dJau = dJauCW;
            }


            // only use the delta if it won't send up past the target
            if (Math.abs(dJau) > JAU_TURN_SPEED)
            {
                dJau = Integer.signum(dJau) * JAU_TURN_SPEED;
            }


            int newOrientation = (JAU_FULL_ROTATION + rlObject.getOrientation() + dJau) % JAU_FULL_ROTATION;

            rlObject.setOrientation(newOrientation);

            dJau = (targetOrientation - newOrientation) % JAU_FULL_ROTATION;
        }

        return dJau == 0;
    }

    static int radToJau(double a)
    {
        int j = (int) Math.round(a / Perspective.UNIT);
        return j & 2047;
    }

    public int getRandom(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min) + min;
    }

    private static SimplePolygon calculateAABB(Client client, Model m, int jauOrient, int x, int y, int z, int zOff)
    {
        AABB aabb = m.getAABB(jauOrient);

        int x1 = aabb.getCenterX();
        int y1 = aabb.getCenterZ();
        int z1 = aabb.getCenterY() + zOff;

        int ex = aabb.getExtremeX();
        int ey = aabb.getExtremeZ();
        int ez = aabb.getExtremeY();

        int x2 = x1 + ex;
        int y2 = y1 + ey;
        int z2 = z1 + ez;

        x1 -= ex;
        y1 -= ey;
        z1 -= ez;

        int[] xa = new int[]{
                x1, x2, x1, x2,
                x1, x2, x1, x2
        };
        int[] ya = new int[]{
                y1, y1, y2, y2,
                y1, y1, y2, y2
        };
        int[] za = new int[]{
                z1, z1, z1, z1,
                z2, z2, z2, z2
        };

        int[] x2d = new int[8];
        int[] y2d = new int[8];

        modelToCanvasCpu(client, 8, x, y, z, 0, xa, ya, za, x2d, y2d);

        return Jarvis.convexHull(x2d, y2d);
    }

    private static void modelToCanvasCpu(Client client, int end, int x3dCenter, int y3dCenter, int z3dCenter, int rotate, int[] x3d, int[] y3d, int[] z3d, int[] x2d, int[] y2d)
    {
        final int
                cameraPitch = client.getCameraPitch(),
                cameraYaw = client.getCameraYaw(),

                pitchSin = SINE[cameraPitch],
                pitchCos = COSINE[cameraPitch],
                yawSin = SINE[cameraYaw],
                yawCos = COSINE[cameraYaw],
                rotateSin = SINE[rotate],
                rotateCos = COSINE[rotate],

                cx = x3dCenter - client.getCameraX(),
                cy = y3dCenter - client.getCameraY(),
                cz = z3dCenter - client.getCameraZ(),

                viewportXMiddle = client.getViewportWidth() / 2,
                viewportYMiddle = client.getViewportHeight() / 2,
                viewportXOffset = client.getViewportXOffset(),
                viewportYOffset = client.getViewportYOffset(),

                zoom3d = client.getScale();

        for (int i = 0; i < end; i++)
        {
            int x = x3d[i];
            int y = y3d[i];
            int z = z3d[i];

            if (rotate != 0)
            {
                int x0 = x;
                x = x0 * rotateCos + y * rotateSin >> 16;
                y = y * rotateCos - x0 * rotateSin >> 16;
            }

            x += cx;
            y += cy;
            z += cz;

            final int
                    x1 = x * yawCos + y * yawSin >> 16,
                    y1 = y * yawCos - x * yawSin >> 16,
                    y2 = z * pitchCos - y1 * pitchSin >> 16,
                    z1 = y1 * pitchCos + z * pitchSin >> 16;

            int viewX, viewY;

            if (z1 < 50)
            {
                viewX = Integer.MIN_VALUE;
                viewY = Integer.MIN_VALUE;
            }
            else
            {
                viewX = (viewportXMiddle + x1 * zoom3d / z1) + viewportXOffset;
                viewY = (viewportYMiddle + y2 * zoom3d / z1) + viewportYOffset;
            }

            x2d[i] = viewX;
            y2d[i] = viewY;
        }
    }
}
