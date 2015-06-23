package edu.uniandes.privateRecsys.onlineRecommender.factorModelRepresentation;

import java.util.HashSet;
import java.util.Set;

import edu.uniandes.privateRecsys.onlineRecommender.FilterElement;
import edu.uniandes.privateRecsys.onlineRecommender.UserModelTrainerPredictor;
import edu.uniandes.privateRecsys.onlineRecommender.Evaluationtesters.RSDataset;
import edu.uniandes.privateRecsys.onlineRecommender.exception.PrivateRecsysException;
import edu.uniandes.privateRecsys.onlineRecommender.postgresdb.PosgresDAO;

public class IncrementalFactorUserItemRepresentationDBBacked extends
		IncrementalFactorUserItemRepresentation {
	
	private PosgresDAO privateDAO;
	public IncrementalFactorUserItemRepresentationDBBacked(RSDataset data,
			int fDimensions, boolean hasPrivateStrategy,
			UserModelTrainerPredictor trainerPredictor) throws PrivateRecsysException {
		super(data, fDimensions, hasPrivateStrategy, trainerPredictor);
		privateDAO= PosgresDAO.getInstance();
	}
	
	@Override
	public Set<Long> getRatedItems(Long userId) {
		HashSet<Long> itemIds= new FilterElement(userId,this.dataSet.getTrainSet()).getElementsFromFile();
		return itemIds;
	}
	

	@Override
	public Set<Long> getPositiveElements(Long userId, String file) {
		HashSet<Long> itemIds= new FilterElement(userId,file).getElementsFromFile();
		return itemIds;
	}

}
