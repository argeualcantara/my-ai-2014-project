package algoritmo;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

import controle.Constantes;

public class Poupador extends ProgramaPoupador {

	// Constantes da matriz de visão
	private static final int SEM_VISAO = -2;
	private static final int FORA_AMBIENTE = -1;
	private static final int PAREDE = 1;
	private static final int BANCO = 3;
	private static final int MOEDA = 4;
	private static final int PASTILHA_PODER = 5;
	private int[] pesos;
	private ArrayList<Integer> esquerda;
	private ArrayList<Integer> cima;
	private ArrayList<Integer> direita;
	private ArrayList<Integer> baixo;
	private ArrayList<Integer> perto;
	private int[] visao;
	private int[] olfato;
	private HashMap<Point, Integer> pontosVisitados;
	private HashMap<String, Point> poupadores;

	public Poupador() {
		this.pontosVisitados = new HashMap<Point, Integer>();
		this.poupadores = new HashMap<String, Point>();
		esquerda = new ArrayList<Integer>(Arrays.asList(10, 11));
		cima = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
		direita = new ArrayList<Integer>(Arrays.asList(12, 13));
		baixo = new ArrayList<Integer>(Arrays.asList(14, 15, 16, 17, 18, 19, 20, 21, 22, 23));
		perto = new ArrayList<Integer>(Arrays.asList(6, 7, 8, 11, 12, 15, 16, 17));
	}

	public int acao() {
		this.pesos = new int[24];
		pesoPontoAtual(sensor.getPosicao());
		analisarLocaisVisitados();
		analisarVisao();
		analisarOlfato();
		return decidirMovimento();
	}

	// reduz o peso do ponto atual
	private void pesoPontoAtual(Point p) {
		if (pontosVisitados.containsKey(p)) {
			int peso = pontosVisitados.get(p);
			peso -= 50;
			pontosVisitados.put(p, peso);
		} else {
			pontosVisitados.put(p, -50);
		}
	}

	// torna mais difícil a visitação de pontos já visitados
	private void analisarLocaisVisitados() {
		Point p = sensor.getPosicao();
		Point cima = new Point(p.x, p.y - 1);
		Point baixo = new Point(p.x, p.y + 1);
		Point esquerda = new Point(p.x - 1, p.y);
		Point direita = new Point(p.x + 1, p.y);
		pesos[7] += (pontosVisitados.get(cima) == null ? 0 : pontosVisitados.get(cima));
		pesos[12] += (pontosVisitados.get(direita) == null ? 0 : pontosVisitados.get(direita));
		pesos[16] += (pontosVisitados.get(baixo) == null ? 0 : pontosVisitados.get(baixo));
		pesos[11] += (pontosVisitados.get(esquerda) == null ? 0 : pontosVisitados.get(esquerda));

	}

	// seta os pesos de acordo com a visão do agente

	public void analisarVisao() {
		// as posições do campo de visão, numeradas de 0 a 23 assim como no PDF
		// do trabalho
		visao = sensor.getVisaoIdentificacao();
		for (int i = 0; i < visao.length; i++) {
			switch (visao[i]) {
			case SEM_VISAO:
				this.pesos[i] += -100;
				break;
			case FORA_AMBIENTE:
				this.pesos[i] += -200;
				break;
			case PAREDE:
				this.pesos[i] += -300;
				break;
			case BANCO:
				this.pesos[i] += 100 * sensor.getNumeroDeMoedas();
				break;
			case MOEDA:
				this.pesos[i] += 1000;
				break;
			default:

				if (visao[i] >= 100) {
					// é outro poupador ou um ladrão
					this.pesos[i] += -1000;
				} else {
					this.pesos[i] += -5;
				}
				break;
			}

		}
	}

	public void analisarOlfato() {
		// primeiro analisa o cheiro deixado pelos ladrões, depois o pelos
		// poupadores
		olfato = sensor.getAmbienteOlfatoLadrao();
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < olfato.length; j++) {

				this.pesos[perto.get(j)] += olfato[j] == 0 ? 200 : -500 * (5 - olfato[j]);
			}
			olfato = sensor.getAmbienteOlfatoPoupador();
		}

	}

	public int decidirMovimento() {

		// considerar a posição do banco
		Point banco = Constantes.posicaoBanco;
		if (banco.x < sensor.getPosicao().x) {
			pesos[11] += sensor.getNumeroDeMoedas() * 10;
		}

		if (banco.x > sensor.getPosicao().x) {

			pesos[12] += sensor.getNumeroDeMoedas() * 10;
		}

		if (banco.y < sensor.getPosicao().y) {

			pesos[16] += sensor.getNumeroDeMoedas() * 10;
		}

		if (banco.y > sensor.getPosicao().y) {

			pesos[7] += sensor.getNumeroDeMoedas() * 10;
		}

		// resumir os pesos para apenas as 4 direções possíveis de movimento

		int pesoEsquerda = somarPesos(esquerda) + pesoObstaculo(11);
		int pesoDireita = somarPesos(direita) + pesoObstaculo(12);
		int pesoCima = somarPesos(cima) + pesoObstaculo(7);
		int pesoBaixo = somarPesos(baixo) + pesoObstaculo(16);

		int[] pesosDirecao = { pesoCima, pesoBaixo, pesoDireita, pesoEsquerda };

		int maiorPeso = -999999;
		int direcao = -1;

		for (int i = 0; i < pesosDirecao.length; i++) {
			if (pesosDirecao[i] > maiorPeso) {
				maiorPeso = pesosDirecao[i];
				direcao = i + 1;
			}
		}

		ArrayList<Integer> pesosIguais = new ArrayList<Integer>();
		// verifica se tem mais de um maiorPeso
		for (int i = 0; i < pesosDirecao.length; i++) {
			if (pesosDirecao[i] == maiorPeso) {
				pesosIguais.add(i + 1);
			}
		}

		if (pesosIguais.size() > 1) {
			int indice = (int) (Math.random() * (pesosIguais.size()));
			direcao = pesosIguais.get(indice);

		}

		/*
		 * os valores de retorno são 0: ficar parado 1: ir pra cima 2: ir pra
		 * baixo 3: ir pra direita 4: ir pra esquerda
		 */

		System.out.println(direcao + " " + pesoCima + " " + pesoBaixo + " " + pesoDireita + " " + pesoEsquerda + " " + sensor.getPosicao());

		if ((direcao < 1) || (direcao > 4)) {
			return 0;
		}
		return direcao;

	}

	private int pesoObstaculo(int posicao) {

		if ((visao[posicao] == PAREDE) || (visao[posicao] == FORA_AMBIENTE) || (visao[posicao] >= 100)) {
			return -2000;
		}

		if ((visao[posicao] == PASTILHA_PODER) && (sensor.getNumeroDeMoedas() < 5)) {
			return -2000;
		}

		if ((visao[posicao] == BANCO) && (sensor.getNumeroDeMoedas() == 0)) {
			return -2000;
		}

		return 0;

	}

	private int somarPesos(ArrayList<Integer> direcao) {
		int soma = 0;
		for (int i : direcao) {

			soma += pesos[i];

		}
		return soma;
	}
	
}