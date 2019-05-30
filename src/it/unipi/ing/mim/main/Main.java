package it.unipi.ing.mim.main;
import java.util.concurrent.ExecutionException;

import com.mathworks.engine.*;
public class Main {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
        MatlabEngine eng;
		try {
			eng = MatlabEngine.startMatlab();
			eng.putVariable("x", 7.0);
	        eng.putVariable("y", 3.0);
	        eng.eval("z = complex(x, y);");
	        double x = eng.getVariable("x");
	        System.out.println(x);
		} catch (EngineException | IllegalArgumentException | IllegalStateException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}