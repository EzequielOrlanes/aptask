import React, { useEffect,useState } from 'react';
import { Link } from "react-router-dom"
import '../style/Home.css';
import Img from "../imagem/ap103.png";
import Ico from "../imagem/icon.svg";
// import { logoutUser } from '../services/api';
import { useNavigate } from 'react-router-dom';


function Home({ loggedUser, setLoggedUser, tasks, rotation }) {
  const navigate = useNavigate();
    const [error, setError] = useState('');

  const handleLogin = (userName) => {
    setLoggedUser(userName);
  };

  useEffect(() => {
    console.log(`Usuário atualizado: ${loggedUser}`);
  }, [loggedUser]);

  // Filtra as tarefas com status "done"
  const tasksDone = tasks.filter(task => task.status === true);

  const [selectedTrashPerson, setSelectedTrashPerson] = React.useState(null);

  const handleTrashResponsibility = (person) => {
    setSelectedTrashPerson(person);
  };

// const handleLogout = async () => {
//   setError('');
//   error('');
//  try {
//             const response = await logoutUser();
//             console.log('Usuário Deslogado:', response.user);
//             navigate('/login');

//              // 5. Mostra feedback visual (opcional)
//         } catch (error) {
//             setError(error.message || 'Erro ao criar conta');
//         }
// }


  return (
    <div>
      <main>
          <div className="logo_task"> <Link to="/home"> <img src={Img} alt="logo da página" /> </Link>
          </div>
          {/* <nav>
              <button id="sign-out-btn" class="icon-btn">
                        <img   onClick={() => handleLogout(loggedUser)} src={Ico} class="icon-img-btn" alt="Logout" />
                  </button>
            </nav>
         */}
        <div className='content'>
          <h2> Moradores: </h2>
          <div className='moradores'>
            <ul>
              <li onClick={() => handleLogin('Ezequiel')}> <Link to="/user" >Eze </Link></li>
              <li onClick={() => handleLogin('Gabriela')}> <Link to="/user" >Gabs </Link></li>
              <li onClick={() => handleLogin('Guilherme')}> <Link to="/user" >Guilhermo </Link></li>
              <li onClick={() => handleLogin('Isabella')}> <Link to="/user" >Bella </Link></li>
            </ul>
          </div>
          <h2> Tarefas já feitas:  </h2>
          <div className='tarefas-feitas'>
            <ul>
            {tasksDone.length > 0 ? (
                tasksDone.map((task, index) => (
                  <li key={index}>{task.task} ✅ 
                <p style={{ fontSize: "10px", fontWeight: "2px" }}>{task.data_and_time} </p>
                  </li>
                ))
              ) : (
                <li>Nenhuma tarefa concluída ainda. 😑 </li>
              )}
            </ul>
          </div>
          <h2> Coleta de lixo: </h2>
          <div className='responsavel-lixo'>
            <ul>
              <li
                onClick={() => handleTrashResponsibility('Ezequiel')}
                className={selectedTrashPerson === 'Ezequiel' ? 'trash-selected' : ''}
              >
                Ezequiel {selectedTrashPerson === 'Ezequiel' && '🗑️'}
              </li>
              <li
                onClick={() => handleTrashResponsibility('Gabriela')}
                className={selectedTrashPerson === 'Gabriela' ? 'trash-selected' : ''}
              >
                Gabriela {selectedTrashPerson === 'Gabriela' && '🗑️'}
              </li>
              <li
                onClick={() => handleTrashResponsibility('Guilherme')}
                className={selectedTrashPerson === 'Guilherme' ? 'trash-selected' : ''}
              >
                Guilherme {selectedTrashPerson === 'Guilherme' && '🗑️'}
              </li>
              <li
                onClick={() => handleTrashResponsibility('Isabella')}
                className={selectedTrashPerson === 'Isabella' ? 'trash-selected' : ''}
              >
                Isabella {selectedTrashPerson === 'Isabella' && '🗑️'}
              </li>
            </ul>
          </div>
          <h2> Avisos: </h2>
          <div className='quadro-avisos'>
            <p> Camêras não estão funcionando.</p>
          </div>
         
        </div>
        <footer>
          <p>Produced by @oezequiel.</p>
        </footer>
      </main>
    </div>
  );
}

export default Home;
