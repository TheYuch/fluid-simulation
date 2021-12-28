package util;

public class Vector2
{

    public float x;
    public float y;
    
    public Vector2()
    {
        this.x = 0f;
        this.y = 0f;
    }
    
    public Vector2(float x, float y)
    {
        this.x = x;
        this.y = y;
    }
    
    public float dot(Vector2 that)
    {
        float sum = 0f;
        sum += this.x * that.x;
        sum += this.y * that.y;
        return sum;
    }
    
    public float magnitude()
    {
        return (float)Math.sqrt(this.dot(this));
    }
    
    public float distanceTo(Vector2 that)
    {
        return this.minus(that).magnitude();
    }
    
    public Vector2 plus(Vector2 that)
    {
        Vector2 c = new Vector2();
        c.x = this.x + that.x;
        c.y = this.y + that.y;
        return c;
    }
    
    public Vector2 minus(Vector2 that)
    {
        Vector2 c = new Vector2();
        c.x = this.x - that.x;
        c.y = this.y - that.y;
        return c;
    }
    
    public Vector2 scale(float factor)
    {
        Vector2 c = new Vector2();
        c.x = factor * this.x;
        c.y = factor * this.y;
        return c;
    }
    
    public Vector2 normalized()
    {
        if (this.magnitude() == 0f)
        {
            return new Vector2();
        }
        return this.scale(1f / this.magnitude());
    }
    
    public void set(Vector2 that)
    {
    	this.x = that.x;
    	this.y = that.y;
    }
    
}
