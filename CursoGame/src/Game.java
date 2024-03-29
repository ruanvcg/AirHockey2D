import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

// Janela em que o jogo ocorrer� de fato:

public class Game extends JPanel{
	private Jogador jogador;
	private Inimigo inimigo;
	private Bolinha bolinha;
	private boolean k_cima = false;
	private boolean k_baixo = false;
	private boolean k_direita = false;
	private boolean k_esquerda = false;
	private BufferedImage bg;
	private long tempoAtual;
	private long tempoAnterior;
	private double deltaTime;
	private double FPS_limit = 60;
	private char estado;
	private BufferedImage splashLogo;
	
	
	public Game() {
		
		addKeyListener(new KeyListener() {
			
			@Override
			public void keyTyped(KeyEvent e) {
				// TODO Auto-generated method stub
				
			}
			@Override
			public void keyReleased(KeyEvent e) {
				if(estado == 'E') {
					switch (e.getKeyCode()) {
					case KeyEvent.VK_UP: k_cima = false; break;
					case KeyEvent.VK_DOWN: k_baixo = false; break;
					case KeyEvent.VK_LEFT: k_esquerda = false; break;
					case KeyEvent.VK_RIGHT: k_direita = false; break;
					}
				}
			}
			@Override
			public void keyPressed(KeyEvent e) {
				
				if(estado == 'E') {
					switch (e.getKeyCode()) {
					case KeyEvent.VK_UP: k_cima = true; break;
					case KeyEvent.VK_DOWN: k_baixo = true; break;
					case KeyEvent.VK_LEFT: k_esquerda = true; break;
					case KeyEvent.VK_RIGHT: k_direita = true; break;
					case KeyEvent.VK_ESCAPE: 
						estado = 'P'; 
						Recursos.getInstance().tocarSomMenu();
						break;
					}
				} else if(estado == 'P') {
					switch (e.getKeyCode()) {
					case KeyEvent.VK_UP:
						Recursos.getInstance().tocarSomMenu();
						
						Recursos.getInstance().pauseOpt = 0;
						break;
					case KeyEvent.VK_DOWN:
						Recursos.getInstance().tocarSomMenu();
						
						Recursos.getInstance().pauseOpt = 1;
						break;
					case KeyEvent.VK_ENTER:
						Recursos.getInstance().tocarSomMenu();
						
						if(Recursos.getInstance().pauseOpt == 0) {
							estado = 'E';
							k_cima = false;
							k_baixo = false;
							k_esquerda = false;
							k_direita = false;
						} else {
							System.exit(0);
						}
						
						break;
					case KeyEvent.VK_ESCAPE:
						Recursos.getInstance().tocarSomMenu();
						
						estado = 'E';
						k_cima = false;
						k_baixo = false;
						k_esquerda = false;
						k_direita = false;
						break;
					}
				}
				
			}
		});
		
		jogador = new Jogador();
		inimigo = new Inimigo();
		bolinha = new Bolinha();
		
		estado = 'S';
		agendarTransicao(3000, 'E');
		
		try {
			bg = ImageIO.read(getClass().getResource("imgs/bg.png"));
			splashLogo = ImageIO.read(getClass().getResource("imgs/logo.png"));
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		setFocusable(true);
		setLayout(null);
		
		/** O que s�o "Threads"?
		� um recurso da linguagem java que permite � aplica��o tire vantagens
		dos modernos processadores com diversos n�cleos de processamento. Em nosso game,
		o uso de Threads se faz necess�rio pricipalmente pelo recurso de pausa de execu��o
		que ele permite (Thread.sleep).
		*/
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				gameloop();
			}
		}).start(); // => start() => run() => [ gameloop() => handlerEvents() => update() => render() => pausa] 
	}
	
	// GAMELOOP ------------------------
	
	// Anima��o do objeto
	public void gameloop(){
		tempoAnterior = System.nanoTime();
		double tempoMinimo = (1e9) / FPS_limit; // dura��o m�nima do quadro (em nanosegundos) 
				
		while (true) {
			tempoAtual = System.nanoTime();
			deltaTime = (tempoAtual - tempoAnterior) * (6e-8);
			handlerEvents();
			update(deltaTime);
			render();
			tempoAnterior = tempoAtual;
			
			try {
				int tempoEspera = (int) ((tempoMinimo - deltaTime) * (1e-6));
				Thread.sleep(tempoEspera);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	// Movimenta��o da bola:
	public void handlerEvents(){
		if(estado == 'E') {
			jogador.handlerEvents(k_cima, k_baixo, k_esquerda, k_direita);
			inimigo.handlerEvents(bolinha);
		}
	}
	
	public void update(double deltaTime){
		if(estado == 'E') {
			jogador.update(deltaTime);
			inimigo.update(deltaTime);
			bolinha.update(deltaTime);
			testeColisoes(deltaTime);
			testeFimJogo();
		}else if(estado == 'G') {
			estado = 'R';
			reiniciar();
			agendarTransicao(2000, 'E');
		}
	}
	
	public void render(){
		repaint(); // Redesenha a tela a cada repeti��o do gameloop()
	}
	
	// OUTROS M�TODOS ------------------
	
	public void testeColisoes(double deltaTime) {
		if (jogador.posX + jogador.raio * 2 >= Principal.largura_tela || jogador.posX <= 0) {
			
			jogador.desmoverX(deltaTime);
		}
		if (jogador.posY + jogador.raio * 2 >= Principal.altura_tela || jogador.posY <= 0) {
			
			jogador.desmoverY(deltaTime);
		}
		
		// COLIS�O DA BOLINHA COM O JOGADOR
		
		double ladoHorizontal = jogador.centroX - bolinha.centroX;
		double ladoVertical = jogador.centroY - bolinha.centroY;
		double hipotenusa = Math.sqrt(Math.pow(ladoHorizontal, 2) + Math.pow(ladoVertical, 2));
		
		if(hipotenusa <= jogador.raio + bolinha.raio) {
			jogador.desmoverX(deltaTime);
			jogador.desmoverY(deltaTime);
			double seno, cosseno;
			cosseno = ladoHorizontal / hipotenusa;
			seno = ladoVertical / hipotenusa;
			bolinha.velX = (- bolinha.velBase) * cosseno;
			bolinha.velY = (- bolinha.velBase) * seno;
			
			Recursos.getInstance().tocarSomBolinha();
		}
		
		// COLIS�O DA BOLINHA COM O INIMIGO
		
		ladoHorizontal = inimigo.centroX - bolinha.centroX;
		ladoVertical = inimigo.centroY - bolinha.centroY;
		hipotenusa = Math.sqrt(Math.pow(ladoHorizontal, 2) + Math.pow(ladoVertical, 2));
		
		if(hipotenusa <= inimigo.raio + bolinha.raio) {
			inimigo.desmoverX(deltaTime);
			inimigo.desmoverY(deltaTime);
			double seno, cosseno;
			cosseno = ladoHorizontal / hipotenusa;
			seno = ladoVertical / hipotenusa;
			bolinha.velX = (- bolinha.velBase) * cosseno;
			bolinha.velY = (- bolinha.velBase) * seno;
			
			Recursos.getInstance().tocarSomBolinha();
		}
		// COLIS�O DO JOGADOR COM O LIMITE DIREITO DO CAMPO
		
		if(jogador.posX <= Principal.limite_direito) {
			jogador.desmoverX(deltaTime);
		}
		
		// COLIS�O DO INIMIGO COM O LIMITE INFERIOR
		
		
		
		// COLIS�O DO INIMIGO COM O LIMITE SUPERIOR
		
		
		
		// COLIS�O DA BOLINHA COM O LADO DIREITO DA TELA
		
		if(bolinha.posX + (bolinha.raio * 2) >= Principal.largura_tela) {
			bolinha.velX = bolinha.velX * -1;
			bolinha.posX = Principal.largura_tela / 2 - (bolinha.raio) + 90;
			Recursos.getInstance().pontosInimigo++;
			
			Recursos.getInstance().tocarSomBolinha();
		}
		
		// COLIS�O DA BOLINHA COM O LADO ESQUERDO DA TELA
		if(bolinha.posX <= 0) {
			bolinha.velX = bolinha.velX * -1;
			bolinha.posX = Principal.largura_tela / 2 - (bolinha.raio) - 90;
			Recursos.getInstance().pontosJogador++;
			
			Recursos.getInstance().tocarSomBolinha();
		}
		
		// COLIS�O DA BOLINHA COM O LADO INFERIOR DA TELA
		
		if(bolinha.posY + (bolinha.raio * 2) >= Principal.altura_tela) {
			bolinha.velY = bolinha.velY * -1;
			bolinha.posY = Principal.altura_tela - (bolinha.raio * 2);
			
			Recursos.getInstance().tocarSomBolinha();
		}
		
		// COLIS�O DA BOLINHA COM O LADO SUPERIOR DA TELA
		
		if(bolinha.posY <= 0) {
			bolinha.velY = bolinha.velY * -1;
			bolinha.posY = 0;
			
			Recursos.getInstance().tocarSomBolinha();
		}
	}
	
	
	public void agendarTransicao(int tempo, char novoEstado) {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(tempo);
				} catch(Exception e) {
					e.printStackTrace();
				}
				estado = novoEstado;
			}
		});
		thread.start();
	}
	
	public void testeFimJogo() {
		if(Recursos.getInstance().pontosJogador == Recursos.getInstance().maxPontos) {
			Recursos.getInstance().msgFim = "VOC� VENCEU!";
			estado = 'G';
		} else if(Recursos.getInstance().pontosInimigo == Recursos.getInstance().maxPontos) {
			Recursos.getInstance().msgFim = "VOC� PERDEU!";
			estado = 'G';
		}
	}
	
	public void reiniciar() {
		// reiniciar os atributos do inimigo
		inimigo.posX = (Principal.largura_tela * (1.0/8.0) - inimigo.raio);
		inimigo.posY = (Principal.altura_tela / 2) - inimigo.raio;
		inimigo.velY = inimigo.velBase;
		Recursos.getInstance().pontosInimigo = 0;
		
		// reiniciar os atributos do jogador
		jogador.posX = (Principal.largura_tela * (7.0/8.0) - jogador.raio);
		jogador.posY = (Principal.altura_tela / 2) - jogador.raio;
		Recursos.getInstance().pontosJogador = 0;
		
		// reiniciar os atributos da bolinha
		bolinha.velX = bolinha.velBase / 2;
		bolinha.velY = bolinha.velBase / 2;
		bolinha.posX = (Principal.largura_tela / 2) - bolinha.raio;
		bolinha.posY = (Principal.altura_tela / 2) - bolinha.raio;
		
		// reiniciar as vari�veis das teclas direcionais
		k_cima = false;
		k_baixo = false;
		k_esquerda = false;
		k_direita = false;
	}
	
	// M�TODO SOBESCRITO ---------------
	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
				RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		super.paintComponent(g2d);
		
		
		if(estado == 'S') {
			g2d.drawImage(splashLogo, 0, 0, null);
		} else if(estado == 'R') {
			g2d.setColor(Color.BLACK);
			g2d.fillRect(0, 0, Principal.largura_tela, Principal.altura_tela);
			g2d.setFont(Recursos.getInstance().fontMenu);
			g2d.setColor(Color.WHITE);
			g2d.drawString(Recursos.getInstance().msgFim, 150, 200);
		} else { // Se estiver n estado EXECUTANDO ou PAUSADO
			
			// desenha o ch�o do cen�rio
			g2d.drawImage(bg, 0, 0, Principal.largura_tela, Principal.altura_tela, null);
			// desenha as marca��es de limite de movimenta��o
			g2d.setColor(Color.GRAY);
			g2d.fillRect(Principal.limite_direito, 0, 5, Principal.altura_tela);
			g2d.fillRect(Principal.limite_esquerdo, 0, 5, Principal.altura_tela);
			
			// Posi��o e dimens�o do obj "Jogador" com par�metros relativos ao obj
			g2d.drawImage(jogador.imgAtual, jogador.af, null);
			// Posi��o e dimens�o do obj "Inimigo" com par�metros relativos ao obj
			g2d.drawImage(inimigo.imgAtual, inimigo.af, null);
			// Posi��o e dimens�o do obj "Bolinha" com par�metros relativos ao obj
			g2d.drawImage(bolinha.img, bolinha.af, null);
			
			if(estado == 'E') { // Executando
				
				// desenha a pontua��o na tela
				g2d.setFont(Recursos.getInstance().fontPontuacao);
				g2d.setColor(Color.WHITE);
				g2d.drawString(Recursos.getInstance().pontosInimigo + "pts", 140, 40);
				g2d.drawString(Recursos.getInstance().pontosJogador + "pts", 440, 40);
			
			} else { // Pausado
				g2d.setColor(new Color(0,0,0,128));
				g2d.fillRect(0, 0, Principal.largura_tela, Principal.altura_tela);
				
				// desenha os elementos do menu pause
				g2d.setFont(Recursos.getInstance().fontMenu);
				g2d.setColor(Color.WHITE);
				g2d.drawString("JOGO PAUSADO", 150, 80);
				g2d.drawString("Continuar", 220, 200);
				g2d.drawString("Sair", 220, 270);
				
				// desenha o seletor de op��es
				if(Recursos.getInstance().pauseOpt == 0) {
					g2d.fillRect(180, 170, 30, 30);
				} else {
					g2d.fillRect(180, 240, 30, 30);
				}
			}
		}
	}
}
