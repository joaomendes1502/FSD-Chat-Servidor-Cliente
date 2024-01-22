import java.io.BufferedWriter;
import java.io.IOException;

public class LoopSessionUpdate extends Thread{

	private String userCliente;
	private BufferedWriter bufferedWriter;
	private int tempoSessionUpdate;

	public LoopSessionUpdate(String userCliente, BufferedWriter bufferedWriter, int tempoSessionUpdate) {
		this.userCliente = userCliente;
		this.bufferedWriter = bufferedWriter;
		this.tempoSessionUpdate = tempoSessionUpdate;
	}

	@Override
	public void run() {

		while(true) {
			try {
				// sleep of 15 sec
				Thread.sleep(tempoSessionUpdate);

				bufferedWriter.write("SESSION_UPDATE_REQUEST: "); // get new updates of messages
				bufferedWriter.newLine();
				bufferedWriter.flush();

			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

	}

}
