import IncidentForm from '../components/IncidentForm';
import { useApp } from '../context/AppContext';

export default function NewIncident() {
  const { addIncident } = useApp();
  return <IncidentForm onSave={addIncident} />;
}
